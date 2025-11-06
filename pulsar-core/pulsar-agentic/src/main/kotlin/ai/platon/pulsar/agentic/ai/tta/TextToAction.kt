package ai.platon.pulsar.agentic.ai.tta

import ai.platon.pulsar.agentic.ai.AgentMessageList
import ai.platon.pulsar.agentic.ai.PromptBuilder.Companion.buildObserveResultSchema
import ai.platon.pulsar.agentic.tools.AgentTool
import ai.platon.pulsar.agentic.tools.AgentTool.TOOL_ALIASES
import ai.platon.pulsar.agentic.tools.ToolCallExecutor
import ai.platon.pulsar.browser.driver.chrome.dom.Locator
import ai.platon.pulsar.browser.driver.chrome.dom.model.BrowserUseState
import ai.platon.pulsar.browser.driver.chrome.dom.model.SnapshotOptions
import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.ai.llm.PromptTemplate
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.serialize.json.pulsarObjectMapper
import ai.platon.pulsar.external.BrowserChatModel
import ai.platon.pulsar.external.ChatModelFactory
import ai.platon.pulsar.external.ModelResponse
import ai.platon.pulsar.skeleton.ai.ObserveElement
import ai.platon.pulsar.skeleton.ai.ToolCall
import ai.platon.pulsar.skeleton.common.llm.LLMUtils
import ai.platon.pulsar.skeleton.crawl.fetch.driver.AbstractWebDriver
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.gson.JsonElement
import java.nio.file.Files

open class TextToAction(
    val conf: ImmutableConfig
) {
    companion object {

        const val SINGLE_ACTION_GENERATION_PROMPT = """
根据动作描述和网页内容，选择最合适一个或多个工具。

## 动作描述

{{ACTION_DESCRIPTIONS}}

---

## 工具列表

```kotlin
{{TOOL_CALL_SPECIFICATION}}
```

---

## 网页内容

网页内容以无障碍树的形式呈现:

{{NANO_TREE_LAZY_JSON}}

---

## 输出

- 仅输出 JSON 内容，无多余文字
- domain 取值 driver
- method 和 arguments 遵循 `## 工具列表` 的函数表达式

动作输出格式：
{{OUTPUT_SCHEMA_ACT}}

---

        """

        fun toActionDescription(ele: ObserveResponseElement, response: ModelResponse): ActionDescription {
            val arguments = ele.arguments
                ?.mapNotNull { arg -> arg?.get("name") to arg?.get("value") }
                ?.filter { it.first != null && it.second != null }
                ?.associate { it.first!! to it.second!! }

            val observeElement = ObserveElement(
                locator = ele.locator?.removeSurrounding("[", "]"),

                screenshotContentSummary = ele.screenshotContentSummary,
                currentPageContentSummary = ele.currentPageContentSummary,
                evaluationPreviousGoal = ele.evaluationPreviousGoal,
                nextGoal = ele.nextGoal,

                toolCall = ToolCall(
                    domain = ele.domain ?: "",
                    method = ele.method ?: "",
                    arguments = arguments?.toMutableMap() ?: mutableMapOf(),
                ),

                modelResponse = response.content,
            )

            return ActionDescription(observeElement = observeElement, modelResponse = response)
        }
    }

    private val logger = getLogger(this)

    val baseDir = AppPaths.get("tta")

    val chatModel: BrowserChatModel get() = ChatModelFactory.getOrCreate(conf)

    val webDriverFile = baseDir.resolve("MiniWebDriver.kt")
    val webDriverToolCallExpressions = mutableListOf<String>()

    init {
        Files.createDirectories(baseDir)

        LLMUtils.copyWebDriverFile(webDriverFile)
        Files.readAllLines(webDriverFile)
            .filter { it.contains(" fun ") }
            .map { it.substringAfterLast("suspend ") }
            .toCollection(webDriverToolCallExpressions)
    }

    /**
     * Generate EXACT ONE WebDriver action with interactive elements.
     *
     * @param actionDescriptions The action descriptions
     * @param driver The driver to use to collect the context, such as interactive elements
     * @return The action description
     * */
    @ExperimentalApi
    open suspend fun generateActions(
        actionDescriptions: String, driver: WebDriver, screenshotB64: String? = null
    ): List<ActionDescription> {
        require(driver is AbstractWebDriver)
        val domService = requireNotNull(driver.domService)

        val options = SnapshotOptions()
        val domState = domService.getDOMState(snapshotOptions = options)

        val promptTemplate = PromptTemplate(SINGLE_ACTION_GENERATION_PROMPT)
        val message = promptTemplate.render(
            mapOf(
                "ACTION_DESCRIPTIONS" to actionDescriptions,
                "TOOL_CALL_SPECIFICATION" to webDriverToolCallExpressions.joinToString("\n"),
                "NANO_TREE_LAZY_JSON" to domState.nanoTreeLazyJson,
                "OUTPUT_SCHEMA_ACT" to buildObserveResultSchema(true),
            )
        )

        val messages = AgentMessageList()
        messages.addUser(message)

        val systemMessage = messages.systemMessages().joinToString("\n")
        val userMessage = messages.userMessages().joinToString("\n")
        val response = if (screenshotB64 != null) {
            chatModel.call(systemMessage, userMessage, null, screenshotB64, "image/jpeg")
        } else {
            chatModel.call(systemMessage, userMessage)
        }

        val mapper = jacksonObjectMapper()
        val content = response.content
        val elements: ObserveResponseElements = mapper.readValue(content)
        return elements.elements?.map { toActionDescription(it, response) } ?: emptyList()
    }

    fun modelResponseToActionDescription(response: ModelResponse): ActionDescription {
        try {
            return modelResponseToActionDescription0(response)
        } catch (e: Exception) {
            logger.warn("Exception while parsing model response", e)
            return ActionDescription(modelResponse = response, errors = e.brief())
        }
    }

    private fun modelResponseToActionDescription0(response: ModelResponse): ActionDescription {
        val content = response.content
        val contentStart = Strings.compactWhitespaces(content.take(30))

        val mapper = pulsarObjectMapper()
        return when {
            contentStart.contains("\"taskComplete\"") -> {
                val complete: ObserveResponseComplete = mapper.readValue(content)
                ActionDescription(
                    isComplete = true,
                    summary = complete.summary,
                    nextSuggestions = complete.nextSuggestions ?: emptyList()
                )
            }

            contentStart.contains("\"elements\"") -> {
                val elements: ObserveResponseElements = mapper.readValue(content)
                elements.elements?.map { TextToAction.toActionDescription(it, response) }?.firstOrNull()
                    ?: ActionDescription(modelResponse = response)
            }

            else -> ActionDescription(modelResponse = response)
        }
    }

    fun reviseActionDescription(action: ActionDescription, browserUseState: BrowserUseState): ActionDescription {
        val observeElement = action.observeElement ?: return action
        var toolCall = observeElement.toolCall ?: return action

        // 1. revised tool call
        val revised = TOOL_ALIASES["${toolCall.domain}.${toolCall.method}"]
        if (revised != null) {
            val (domain2, method2) = revised.split(".")
            toolCall = toolCall.copy(domain = domain2, method = method2)
        }

        // 2. revise selector
        val domain = toolCall.domain
        val method = toolCall.method

        val locator = observeElement.locator
        val arguments = toolCall.arguments

        var fbnLocator: Locator? = null
        if (method in AgentTool.SELECTOR_ACTIONS) {
            fbnLocator = browserUseState.domState.getAbsoluteFBNLocator(locator)
            if (!locator.isNullOrBlank() && fbnLocator == null) {
                logger.warn("FBN locator not found. method={}, locator={}", method, locator)
            }
        }

        val node = if (fbnLocator != null) {
            browserUseState.domState.locatorMap[fbnLocator]
        } else null

        val fbnSelector = fbnLocator?.absoluteSelector

        fbnSelector?.let { arguments["selector"] = it }

        // CSS friendly expression
        val cssSelector = node?.cssSelector()
        val expression = ToolCallExecutor.toolCallToExpression(toolCall)
        val cssFriendlyExpression = if (locator != null && cssSelector != null) {
            expression?.replace(locator, cssSelector)
        } else null

        // 3. copy new object
        val revisedObserveElement = observeElement.copy(
            node = node,
            backendNodeId = node?.backendNodeId,
            toolCall = toolCall,
            cssSelector = cssSelector,
            expression = expression,
            cssFriendlyExpression = cssFriendlyExpression,
        )

        return action.copy(observeElement = revisedObserveElement)
    }

    private fun jsonElementToKotlin(e: JsonElement): Any? = when {
        e.isJsonNull -> null
        e.isJsonPrimitive -> {
            val p = e.asJsonPrimitive
            when {
                p.isBoolean -> p.asBoolean
                p.isNumber -> {
                    val num = p.asNumber
                    val d = num.toDouble()
                    val i = num.toInt()
                    if (d == i.toDouble()) i else d
                }

                else -> p.asString
            }
        }

        e.isJsonArray -> e.asJsonArray.map { jsonElementToKotlin(it) }
        e.isJsonObject -> e.asJsonObject.entrySet().associate { it.key to jsonElementToKotlin(it.value) }
        else -> null
    }
}
