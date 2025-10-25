package ai.platon.pulsar.agentic.ai.tta

import ai.platon.pulsar.agentic.ai.PromptBuilder
import ai.platon.pulsar.agentic.ai.SimpleMessage
import ai.platon.pulsar.agentic.ai.SimpleMessageList
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

    @ExperimentalApi
    open suspend fun generate(
        messages: SimpleMessageList,
        browserUseState: BrowserUseState,
        screenshotB64: String? = null
    ): ActionDescription {
        try {
            val response = generateResponse(messages, browserUseState, screenshotB64, 1)

            return modelResponseToActionDescription(response, browserUseState)
        } catch (e: Exception) {
            val errorResponse = ModelResponse(
                "Unknown exception" + e.brief(), ResponseState.OTHER
            )
            return ActionDescription(errorResponse)
        }
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
        val messages = SimpleMessageList()
        messages.add("user", instruction)
        return generateResponse(messages, browserUseState, screenshotB64, toolCallLimit)
    }

    @ExperimentalApi
    open suspend fun generateResponse(
        messages: SimpleMessageList, browserUseState: BrowserUseState, screenshotB64: String? = null, toolCallLimit: Int = 100,
    ): ModelResponse {
        var overallGoal = messages.find("overallGoal")?.content ?: ""
        overallGoal = StringUtils.substringBetween(overallGoal, "<overallGoal>", "</overallGoal>")
        val params = ObserveParams(overallGoal, browserUseState = browserUseState, returnAction = true, logInferenceToFile = true)

        PromptBuilder().buildObserveUserMessage(messages, params)

        val systemMessage = messages.systemMessages().joinToString("\n")
        val userMessage = messages.userMessages().joinToString("\n")
        val response = if (screenshotB64 != null) {
            chatModel.call(systemMessage, userMessage, null, screenshotB64, "image/jpeg")
        } else {
            chatModel.call(systemMessage, userMessage)
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

- 工具调用时，将 `locator` 视为 `selector`
- 确保 `locator` 与对应的无障碍树节点属性完全匹配，可以定位该节点
- 不提供不能确定的参数
- 要求 json 输出时，禁止包含任何额外文本

2) 任务完成输出：

{"taskComplete":bool,"summary":string,"keyFindings":[string],"nextSuggestions":[string]}

## 安全要求：
- 仅操作可见的交互元素
- 遇到验证码或安全提示时停止执行

## 工具规范：

```kotlin
${ToolCallExecutor.TOOL_CALL_LIST}
```

- 注意：用户难以区分按钮和链接
- 若操作与页面无关，返回空数组
- 只返回一个最相关的操作
- 按键操作（如"按回车"），用press方法（参数为"A"/"Enter"/"Space"）。特殊键首字母大写。。不要模拟点击屏幕键盘上的按键
- 仅对特殊按键（如 Enter、Tab、Escape）进行首字母大写
- 如果需要操作前一页面，但已跳转，使用 `goBack`

## 无障碍树（Accessibility Tree）说明：

无障碍树包含页面 DOM 关键节点的主要信息，包括节点文本内容，可见性，可交互性，坐标和尺寸等。

- 节点唯一定位符 `locator` 由两个整数组成。
- 所有节点可见，除非 `invisible` == true 显示指定。
- 除非显式指定，`scrollable` 为 false, `interactive` 为 false。
- 对于坐标和尺寸，若未显式赋值，则视为 `0`。涉及属性：`clientRects`, `scrollRects`, `bounds`。

请基于当前页面截图、无障碍树与历史动作，规划下一步（严格单步原子动作）。

        """.trimIndent()

        fun buildOperatorSystemPrompt(): String {
            return """
$TTA_AGENT_GUIDE_SYSTEM_PROMPT
        """.trimIndent()
        }
    }
}
