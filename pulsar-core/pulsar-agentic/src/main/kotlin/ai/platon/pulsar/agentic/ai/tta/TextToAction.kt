package ai.platon.pulsar.agentic.ai.tta

import ai.platon.pulsar.agentic.ai.PromptBuilder
import ai.platon.pulsar.agentic.ai.agent.ObserveParams
import ai.platon.pulsar.browser.driver.chrome.dom.model.BrowserUseState
import ai.platon.pulsar.browser.driver.chrome.dom.model.SnapshotOptions
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.ExperimentalApi
import ai.platon.pulsar.common.brief
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.external.BrowserChatModel
import ai.platon.pulsar.external.ChatModelFactory
import ai.platon.pulsar.external.ModelResponse
import ai.platon.pulsar.external.ResponseState
import ai.platon.pulsar.protocol.browser.driver.cdt.PulsarWebDriver
import ai.platon.pulsar.skeleton.ai.support.ToolCall
import ai.platon.pulsar.skeleton.crawl.fetch.driver.ToolCallExecutor
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import org.apache.commons.lang3.StringUtils
import java.nio.file.Files

open class TextToAction(
    val conf: ImmutableConfig
) {
    private val logger = getLogger(this)

    val baseDir = AppPaths.get("tta")

    val chatModel: BrowserChatModel get() = ChatModelFactory.getOrCreate(conf)

    init {
        Files.createDirectories(baseDir)
    }

    /**
     * Generate EXACT ONE WebDriver action with interactive elements.
     *
     * @param instruction The instruction
     * @param driver The driver to use to collect the context, such as interactive elements
     * @return The action description
     * */
    @ExperimentalApi
    open suspend fun generate(
        instruction: String,
        driver: WebDriver,
        screenshotB64: String? = null
    ): ActionDescription {
        require(driver is PulsarWebDriver) { "PulsarWebDriver is required to use agents" }
        val browserUseState = driver.domService.getBrowserUseState(snapshotOptions = SnapshotOptions())

        return generate(instruction, browserUseState, screenshotB64)
    }

    /**
     * Generate EXACT ONE WebDriver action with interactive elements.
     *
     * @param instruction The action description with plain text
     * @param driver The driver to use to collect the context, such as interactive elements
     * @return The action description
     * */
    @ExperimentalApi
    open suspend fun generate(
        instruction: String,
        browserUseState: BrowserUseState,
        screenshotB64: String? = null
    ): ActionDescription {
        try {
            return generateWithToolCallSpecs(instruction, browserUseState, screenshotB64, 1)
        } catch (e: Exception) {
            val errorResponse = ModelResponse(
                "Unknown exception" + e.brief(), ResponseState.OTHER
            )
            return ActionDescription(errorResponse)
        }
    }

    @ExperimentalApi
    open suspend fun generateResponse(
        instruction: String, browserUseState: BrowserUseState, screenshotB64: String? = null, toolCallLimit: Int = 100,
    ): ModelResponse {
        // instruction from agent:
        // systemMsg + userMsg + screenshot reminder
        //
        // + [agent general guide + overall goal]
        // + [history + atom operation guide + completion condition + overall goal]
        // + [screenshot reminder]
        val fromAgent = instruction.contains(TTA_AGENT_SYSTEM_PROMPT_PREFIX_20)
        val agentGuidSystemMsg = if (fromAgent) instruction else buildOperatorSystemPrompt(instruction)

        val overallGoal = StringUtils.substringBetween(instruction, "<overallGoal>", "</overallGoal>")
        val params = ObserveParams(overallGoal, browserUseState = browserUseState, returnAction = true, logInferenceToFile = true)
        // observe guide:
        // + [instruction] + DOM + browser state + schema?
        // + observe guide
        val observeGuide = PromptBuilder().buildObserveUserMessage(params)
        // tool specs + [observe guide] + completion guide
        val userMessage = buildBrowserUseStatePrompt(observeGuide, toolCallLimit)
        val response = if (screenshotB64 != null) {
            chatModel.call(agentGuidSystemMsg, userMessage, null, screenshotB64, "image/jpeg")
        } else {
            chatModel.call(agentGuidSystemMsg, userMessage)
        }

        return response
    }

    @ExperimentalApi
    private suspend fun generateWithToolCallSpecs(
        instruction: String, browserUseState: BrowserUseState, screenshotB64: String? = null, toolCallLimit: Int = 100,
    ): ActionDescription {
        val response = generateResponse(instruction, browserUseState, screenshotB64, toolCallLimit)

        return modelResponseToActionDescription(response, browserUseState)
    }

    protected fun modelResponseToActionDescription(response: ModelResponse, browserUseState: BrowserUseState? = null): ActionDescription {
        val content = response.content
        // Try new JSON formats first
        try {
            val root = JsonParser.parseString(content)
            if (root.isJsonObject) {
                val obj = root.asJsonObject

                // Completion shape: { isComplete, summary, suggestions }
                if (obj.has("isComplete")) {
                    val isComplete = obj.get("isComplete")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: false
                    val summary = obj.get("summary")?.takeIf { it.isJsonPrimitive }?.asString
                    val suggestionsEl = obj.get("suggestions")
                    val suggestions = if (suggestionsEl?.isJsonArray == true) {
                        suggestionsEl.asJsonArray.mapNotNull { if (it.isJsonPrimitive) it.asString else null }
                    } else emptyList()

                    return ActionDescription(
                        cssFriendlyExpressions = emptyList(),
                        modelResponse = response,
                        isComplete = isComplete,
                        summary = summary,
                        suggestions = suggestions
                    )
                }

                // Action shape: { elements: [ { locator, description, method, arguments: [{name,value}] } ] }
                if (obj.has("elements") && obj.get("elements").isJsonArray) {
                    val arr = obj.getAsJsonArray("elements")
                    val first = arr.firstOrNull()?.let { if (it.isJsonObject) it.asJsonObject else null }
                    if (first != null) {
                        val method = first.get("method")?.takeIf { it.isJsonPrimitive }?.asString
                        val locator = first.get("locator")?.takeIf { it.isJsonPrimitive }?.asString
                        if (!method.isNullOrBlank()) {
                            val argsMap = linkedMapOf<String, Any?>()
                            if (!locator.isNullOrBlank()) {
                                argsMap["selector"] = locator
                            }
                            val argsArr = first.get("arguments")
                            if (argsArr?.isJsonArray == true) {
                                argsArr.asJsonArray.forEach { argEl ->
                                    if (argEl.isJsonObject) {
                                        val name = argEl.asJsonObject.get("name")?.takeIf { it.isJsonPrimitive }?.asString
                                        val valueEl = argEl.asJsonObject.get("value")
                                        if (!name.isNullOrBlank()) {
                                            argsMap[name] = if (valueEl != null) jsonElementToKotlin(valueEl) else null
                                        }
                                    } else if (argEl.isJsonPrimitive) {
                                        // No name provided; skip to keep strict schema
                                    }
                                }
                            }

                            return createActionDescription(response, method, argsMap, locator, browserUseState)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            logger.debug("Model response is not valid JSON or parse failed: {}", e.message)
        }

        // Fallback: plain driver.* expressions
        val expressions = content.split("\n").map { it.trim() }
            .filter { it.startsWith("driver.") && it.contains("(") }
        return ActionDescription(response, expressions = expressions)
    }

    private fun createActionDescription(
        response: ModelResponse,
        method: String,
        argsMap: Map<String, Any?>,
        locator: String?,
        browserUseState: BrowserUseState? = null,
    ): ActionDescription {
        var toolCall = ToolCall("driver", method, argsMap)

        if (browserUseState == null) {
            return ActionDescription(modelResponse = response, toolCall = toolCall)
        }

        val fbnLocator = browserUseState.domState.getAbsoluteFBNLocator(locator)
        val node = if (fbnLocator != null) browserUseState.domState.locatorMap[fbnLocator] else null
        val fbnSelector = fbnLocator?.absoluteSelector

        val revisedArgsMap = argsMap.toMutableMap()
        if (revisedArgsMap.contains("selector")) {
            revisedArgsMap["selector"] = fbnSelector
            toolCall = ToolCall("driver", method, revisedArgsMap)
        }

        // CSS friendly expression
        val cssSelector = node?.cssSelector()
        val expression = ToolCallExecutor.toolCallToExpression(toolCall)
        val cssFriendlyExpression = if (locator != null && cssSelector != null) {
            expression?.replace(locator, cssSelector)
        } else null

        return ActionDescription(
            modelResponse = response,
            toolCall = toolCall,
            locator = fbnSelector,
            xpath = node?.xpath,
            cssSelector = cssSelector,
            cssFriendlyExpressions = listOfNotNull(cssFriendlyExpression),
            expressions = listOfNotNull(expression),
            node = node
        )
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

    companion object {

        val TTA_AGENT_GUIDE_SYSTEM_PROMPT = """
你是一个网页通用代理，目标是基于用户目标一步一步完成任务。

重要指南：
1) 将复杂动作拆成原子步骤；
2) 一次仅做一个动作（如：单击一次、输入一次、选择一次）；
3) 不要在一步中合并多个动作；
4) 多个动作用多步表达；
5) 始终验证目标元素存在且可见后再执行操作；
6) 遇到错误时尝试替代方案或优雅终止；

## 输出严格使用以下两种 JSON 之一：

1) 动作输出（仅含一个元素）：
{
  "elements": [
    {
      "locator": string,
      "description": string,
      "method": string,
      "arguments": [ { "name": string, "value": string } ]
    }
  ]
}

2) 任务完成输出：
{
  "isComplete": true,
  "summary": string,
  "suggestions": [ string ]
}

## 安全要求：
- 仅操作可见的交互元素
- 遇到验证码或安全提示时停止执行

## 工具规范：
```
${ToolCallExecutor.TOOL_CALL_LIST}
```

        """.trimIndent()

        val TTA_AGENT_SYSTEM_PROMPT_PREFIX_20 = TTA_AGENT_GUIDE_SYSTEM_PROMPT.take(20)

        /**
         * Brief:
         * instruction + DOM + browser state + schema + completion guide
         * */
        fun buildBrowserUseStatePrompt(observeMessage: PromptBuilder.SimpleMessage, toolCallLimit: Int = 1): String {
            val prompt = """
每次最多调用 $toolCallLimit 个工具。

${observeMessage.content}

如果总体目标已经达成，则严格按如下格式输出 JSON 信息：

{
  "isComplete": true,
  "summary": string,
  "suggestions": [string]
}

"""

            return prompt
        }

        fun buildOperatorSystemPrompt(overallGoal: String): String {
            return """
$TTA_AGENT_GUIDE_SYSTEM_PROMPT
        """.trimIndent()
        }

    }
}
