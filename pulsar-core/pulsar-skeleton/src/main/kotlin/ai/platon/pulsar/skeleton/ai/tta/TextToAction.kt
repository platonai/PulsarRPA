package ai.platon.pulsar.skeleton.ai.tta

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.ai.llm.PromptTemplate
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.external.ChatModelFactory
import ai.platon.pulsar.external.ModelResponse
import ai.platon.pulsar.external.ResponseState
import ai.platon.pulsar.skeleton.common.llm.LLMUtils
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.runBlocking
import org.apache.hadoop.record.compiler.generated.Rcc.driver
import java.nio.file.Files

data class InteractiveElement(
    val id: String,
    val tagName: String,
    val selector: String,
    val text: String,
    val type: String?,
    val href: String?,
    val className: String?,
    val placeholder: String?,
    val value: String?,
    val isVisible: Boolean,
    val bounds: ElementBounds
) {
    val description: String
        get() = buildString {
            append("[$tagName")
            if (type != null) append(" type='$type'")
            append("] ")
            if (text.isNotBlank()) append("'$text' ")
            if (placeholder != null) append("placeholder='$placeholder' ")
            if (value != null) append("value='$value' ")
            append("selector='$selector'")
        }

    override fun toString() = description
}

data class ElementBounds(
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double
)

data class ActionDescription(
    val functionCalls: List<String>,
    val selectedElement: InteractiveElement?,
    val modelResponse: ModelResponse,
) {
}

data class InstructionResult(
    val functionCalls: List<String>,
    val functionResults : List<Any?>,
    val modelResponse: ModelResponse,
) {
    companion object {
        val LLM_NOT_AVAILABLE = InstructionResult(
            listOf(),
            listOf(),
            modelResponse = ModelResponse.LLM_NOT_AVAILABLE,
        )
    }
}

data class WebDriverMethodInfo(
    val methodSignature: String,
    val fullDescription: String
)

data class MethodSelectionResult(
    val selectedMethods: List<String>,
    val modelResponse: ModelResponse
)

class TextToAction(val conf: ImmutableConfig) {
    private val logger = getLogger(this)
    private val model = ChatModelFactory.getOrCreateOrNull(conf)

    val baseDir = AppPaths.get("tta")

    val webDriverSourceCodeFile = baseDir.resolve("MiniWebDriver.kt")
    var webDriverSourceCode: String
        private set
    var webDriverSourceCodeUseMessage: String
        private set

    val TOOL_CALL_LIST = """
- click(selector: String)
- fill(selector: String, text: String)
- navigateTo(url: String)
- scrollDown(count: Int = 1)
- scrollUp(count: Int = 1)
- scrollToMiddle(ratio: Double = 0.5)
- waitForSelector(selector: String, timeoutMillis: Long = 5000)
- check(selector: String)
- uncheck(selector: String)
    """.trimIndent()

    // Tool-use helpers -------------------------------------------------------------------------
    internal data class ToolCall(val name: String, val args: Map<String, Any?>)

    private fun buildToolUsePrompt(
        userPrompt: String,
        interactiveElements: List<InteractiveElement>,
        toolCallLimit: Int = 100,
    ): String {
        val promptPrefix = """
你现在以工具调用模式工作。给定用户指令, 只返回可执行的工具调用 JSON。
$TOOL_CALL_LIST

返回格式严格为(不要多余文字):
{
  "tool_calls":[
    {"name":"click","args":{"selector":"#submit-btn"}},
    {"name":"fill","args":{"selector":"#search-input","text":"Hello"}}
  ]
}

规则:
1. 仅返回 JSON
2. 若无法确定操作, 返回 {"tool_calls":[]}
3. 参数缺失时不要臆造 selector
4. 不返回注释/Markdown/代码块

    """.trimIndent()

        val prompt = buildString {
            append(promptPrefix)

            appendLine("每次最多调用 $toolCallLimit 个工具")

            if (interactiveElements.isNotEmpty()) {
                appendLine("可交互元素列表: ")
                interactiveElements.forEach { e -> appendLine(e.toString()) }
            }

            appendLine()
            appendLine("用户指令: $userPrompt")
        }

        return prompt
    }

    // Proper JSON parsing with Gson instead of ad-hoc regex
    internal fun parseToolCalls(json: String): List<ToolCall> {
        if (json.isBlank()) return emptyList()
        return try {
            val root = JsonParser.parseString(json)
            if (!root.isJsonObject) return emptyList()
            val arr = root.asJsonObject.getAsJsonArray("tool_calls") ?: return emptyList()
            arr.mapNotNull { el ->
                if (!el.isJsonObject) return@mapNotNull null
                val obj = el.asJsonObject
                val name = obj.get("name")?.takeIf { it.isJsonPrimitive }?.asString ?: return@mapNotNull null
                val argsObj: JsonObject = obj.getAsJsonObject("args") ?: JsonObject()
                val args = mutableMapOf<String, Any?>()
                for ((k, v) in argsObj.entrySet()) {
                    args[k] = jsonElementToKotlin(v)
                }
                ToolCall(name, args)
            }
        } catch (e: Exception) {
            logger.warn("Failed to parse tool calls: {}", e.message)
            emptyList()
        }
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

    internal fun toolCallToDriverLine(tc: ToolCall): String? = when (tc.name) {
        "click" -> tc.args["selector"]?.let { "driver.click(\"$it\")" }
        "fill" -> tc.args["selector"]?.let { s -> "driver.fill(\"$s\", \"${tc.args["text"] ?: ""}\")" }
        "navigateTo" -> tc.args["url"]?.let { "driver.navigateTo(\"$it\")" }
        "scrollDown" -> "driver.scrollDown(${tc.args["count"] ?: 1})"
        "scrollUp" -> "driver.scrollUp(${tc.args["count"] ?: 1})"
        "scrollToMiddle" -> "driver.scrollToMiddle(${tc.args["ratio"] ?: 0.5})"
        "waitForSelector" -> tc.args["selector"]?.let { sel -> "driver.waitForSelector(\"$sel\", ${(tc.args["timeoutMillis"] ?: 5000)}L)" }
        "check" -> tc.args["selector"]?.let { "driver.check(\"$it\")" }
        "uncheck" -> tc.args["selector"]?.let { "driver.uncheck(\"$it\")" }
        else -> null
    }

    init {
        Files.createDirectories(baseDir)

        LLMUtils.copyWebDriverFile(webDriverSourceCodeFile)
        webDriverSourceCode = Files.readAllLines(webDriverSourceCodeFile)
            .filter { it.contains(" fun ") }
            .joinToString("\n")
        webDriverSourceCodeUseMessage = PromptTemplate(WEB_DRIVER_SOURCE_CODE_USE_MESSAGE_TEMPLATE)
            .render(mapOf("webDriverSourceCode" to webDriverSourceCode))
    }

    /**
     * Generate EXACT ONE WebDriver action with interactive elements.
     *
     * @param command The action description with plain text
     * @param driver The driver to use to collect the context, such as interactive elements
     * @return The action description
     * */
    fun generateWebDriverAction(
        command: String,
        driver: WebDriver
    ): ActionDescription {
        try {
            val interactiveElements = extractInteractiveElements(driver)

            return generateWebDriverAction(command, interactiveElements)
        } catch (e: Exception) {
            val errorResponse = ModelResponse("""
                suspend fun llmGeneratedFunction(driver: WebDriver) {
                    // Error occurred during optimization: ${e.message}
                }
            """.trimIndent(), ResponseState.OTHER)
            return ActionDescription(emptyList(), null, errorResponse)
        }
    }

    /**
     * Generate EXACT ONE WebDriver action with interactive elements.
     *
     * @param command The action description with plain text
     * @param driver The driver to use to collect the context, such as interactive elements
     * @return The action description
     * */
    fun generateWebDriverAction(
        command: String,
        interactiveElements: List<InteractiveElement> = listOf()
    ): ActionDescription {
        return generateWebDriverActionsWithToolCallSpecs(command, interactiveElements, 1)
    }

    fun generateWebDriverActionsWithToolCallSpecs(
        command: String,
        interactiveElements: List<InteractiveElement> = listOf(),
        toolCallLimit: Int = 100,
    ): ActionDescription {
        val toolPrompt = buildToolUsePrompt(command, interactiveElements, toolCallLimit)
        val response = model?.call(toolPrompt) ?: ModelResponse.LLM_NOT_AVAILABLE
        val toolCalls = parseToolCalls(response.content).take(toolCallLimit)
        val functionCalls = if (toolCalls.isNotEmpty()) {
            toolCalls.mapNotNull { toolCallToDriverLine(it) }
        } else {
            response.content.split("\n").map { it.trim() }.filter { it.startsWith("driver.") && it.contains("(") }
        }
        return ActionDescription(functionCalls, null, response)
    }

    fun generateWebDriverActionsWithSourceCode(
        command: String,
        interactiveElements: List<InteractiveElement> = listOf()
    ): ModelResponse {
        val prompt = buildString {
            appendLine(webDriverSourceCodeUseMessage)

            if (interactiveElements.isNotEmpty()) {
                appendLine("可交互元素列表：")
                interactiveElements.forEach { appendLine(it) }
            }

            appendLine(command)
        }

        return model?.call(prompt) ?: ModelResponse.LLM_NOT_AVAILABLE
    }

    /**
     * Parse JavaScript result into InteractiveElement objects
     */
    private fun parseInteractiveElements(jsResult: Any?): List<InteractiveElement> {
        if (jsResult == null) return emptyList()

        try {
            // 处理 JavaScript 返回的结果
            // 在真实场景中，jsResult 通常是一个包含元素信息的 Map 或 List
            when (jsResult) {
                is List<*> -> {
                    return jsResult.mapNotNull { item ->
                        parseElementFromMap(item as? Map<String, Any?>)
                    }
                }
                is Map<*, *> -> {
                    // 如果返回的是单个对象，可能包含数组
                    val elements = jsResult["elements"] as? List<*>
                    return elements?.mapNotNull { item ->
                        parseElementFromMap(item as? Map<String, Any?>)
                    } ?: emptyList()
                }
                is String -> {
                    // 如果返回的是 JSON 字符串，需要解析
                    return parseElementsFromJsonString(jsResult)
                }
            }
        } catch (e: Exception) {
            logger.warn("Error parsing interactive elements: {}", e.message)
        }

        return emptyList()
    }

    /**
     * Parse a single element from a Map structure
     */
    private fun parseElementFromMap(elementMap: Map<String, Any?>?): InteractiveElement? {
        if (elementMap == null) return null

        try {
            val bounds = elementMap["bounds"] as? Map<String, Any?> ?: return null

            return InteractiveElement(
                id = elementMap["id"] as? String ?: "",
                tagName = elementMap["tagName"] as? String ?: "",
                selector = elementMap["selector"] as? String ?: "",
                text = (elementMap["text"] as? String ?: "").take(100),
                type = elementMap["type"] as? String,
                href = elementMap["href"] as? String,
                className = elementMap["className"] as? String,
                placeholder = elementMap["placeholder"] as? String,
                value = elementMap["value"] as? String,
                isVisible = elementMap["isVisible"] as? Boolean ?: false,
                bounds = ElementBounds(
                    x = (bounds["x"] as? Number)?.toDouble() ?: 0.0,
                    y = (bounds["y"] as? Number)?.toDouble() ?: 0.0,
                    width = (bounds["width"] as? Number)?.toDouble() ?: 0.0,
                    height = (bounds["height"] as? Number)?.toDouble() ?: 0.0
                )
            )
        } catch (e: Exception) {
            logger.warn("Error parsing element from map: {}", e.message)
            return null
        }
    }

    /**
     * Parse elements from JSON string using Gson
     */
    internal fun parseElementsFromJsonString(jsonString: String): List<InteractiveElement> {
        if (jsonString.isBlank()) return emptyList()
        return try {
            val root = JsonParser.parseString(jsonString)
            val arr = when {
                root.isJsonArray -> root.asJsonArray
                root.isJsonObject && root.asJsonObject.get("elements")?.isJsonArray == true -> root.asJsonObject.getAsJsonArray("elements")
                else -> return emptyList()
            }
            arr.mapNotNull { el ->
                if (!el.isJsonObject) return@mapNotNull null
                val obj = el.asJsonObject
                val boundsObj = obj.get("bounds")?.takeIf { it.isJsonObject }?.asJsonObject
                val bounds = ElementBounds(
                    x = boundsObj?.get("x")?.asDouble ?: 0.0,
                    y = boundsObj?.get("y")?.asDouble ?: 0.0,
                    width = boundsObj?.get("width")?.asDouble ?: 0.0,
                    height = boundsObj?.get("height")?.asDouble ?: 0.0
                )
                InteractiveElement(
                    id = obj.get("id")?.asString ?: "",
                    tagName = obj.get("tagName")?.asString ?: "",
                    selector = obj.get("selector")?.asString ?: "",
                    text = obj.get("text")?.asString?.take(100) ?: "",
                    type = obj.get("type")?.asString,
                    href = obj.get("href")?.asString,
                    className = obj.get("className")?.asString,
                    placeholder = obj.get("placeholder")?.asString,
                    value = obj.get("value")?.asString,
                    isVisible = obj.get("isVisible")?.asBoolean ?: false,
                    bounds = bounds
                )
            }
        } catch (e: Exception) {
            logger.warn("Failed to parse elements JSON: {}", e.message)
            emptyList()
        }
    }

    // Make extraction helpers internal for tests
    internal fun extractSelector(command: String): String? {
        val patterns = listOf(
            // ID selectors
            """['"](#[^'"]+)['"]""".toRegex(),
            // Class selectors (allow dot followed by any non-quote/non-space chars)
            """['"](\.[^'"\s]+)['"]""".toRegex(),
            // Attribute selectors
            """['"](\[[^]]+])['"]""".toRegex(),
            // Fallback: any quoted token starting with a letter
            """['"]([a-zA-Z][^'"]*?)['"]""".toRegex()
        )
        for (pattern in patterns) {
            val match = pattern.find(command)
            if (match != null) return match.groupValues[1]
        }
        return null
    }

    internal fun extractSelectorAndText(command: String): Pair<String?, String?> {
        val patterns = listOf(
            """fill\s*\(\s*['"]([^'"]+)['"]\s*,\s*['"]([^'"]*)['"]\s*\)""".toRegex(),
            """type\s*\(\s*['"]([^'"]+)['"]\s*,\s*['"]([^'"]*)['"]\s*\)""".toRegex()
        )
        for (pattern in patterns) {
            val match = pattern.find(command)
            if (match != null && match.groupValues.size >= 3) return Pair(match.groupValues[1], match.groupValues[2])
        }
        return Pair(null, null)
    }

    internal fun extractRatio(command: String): Double? {
        val patterns = listOf(
            """scrollToMiddle\s*\(\s*(\d*\.?\d+)\s*\)""".toRegex(),
            """(\d*\.?\d+)""".toRegex()
        )
        for (pattern in patterns) {
            val match = pattern.find(command)
            if (match != null) return match.groupValues[1].toDoubleOrNull()
        }
        return null
    }

    internal fun extractCount(command: String): Int? {
        val patterns = listOf(
            """(?:scrollDown|scrollUp)\s*\(\s*(\d+)\s*\)""".toRegex(),
            """(\d+)""".toRegex()
        )
        for (pattern in patterns) {
            val match = pattern.find(command)
            if (match != null) return match.groupValues[1].toIntOrNull()
        }
        return null
    }

    internal fun extractSelectorAndTimeout(command: String): Pair<String?, Long?> {
        val selectorPattern = """waitForSelector\s*\(\s*['"]([^'"]+)['"]""".toRegex()
        val timeoutPattern = """(\d+)L?""".toRegex()
        val selector = selectorPattern.find(command)?.groupValues?.get(1)
        val timeout = timeoutPattern.findAll(command).lastOrNull()?.groupValues?.get(1)?.toLongOrNull()
        return Pair(selector, timeout)
    }

    internal fun extractUrl(command: String): String? {
        val patterns = listOf(
            """navigateTo\s*\(\s*['"]([^'"]+)['"]""".toRegex(),
            """['"]((https?://[^'"]+))['"]""".toRegex()
        )
        for (pattern in patterns) {
            val match = pattern.find(command)
            if (match != null) return match.groupValues[1]
        }
        return null
    }

    // Newly reintroduced helpers --------------------------------------------------------------
    fun extractInteractiveElements(driver: WebDriver): List<InteractiveElement> {
        return runBlocking {
            val result = driver.evaluate(extractElementsScript)
            parseInteractiveElements(result)
        }
    }

    companion object {
        val WEB_DRIVER_SOURCE_CODE_USE_MESSAGE_TEMPLATE = """
以下是浏览器自动化的 WebDriver API 接口及其注释，你可以使用这些接口来控制浏览器。

{{webDriverSourceCode}}

WebDriver 代码结束。

如果用户要求你执行浏览器操作，你需要生成一个函数实现用户的需求。
该函数接收一个 WebDriver 对象，你仅允许使用 WebDriver 接口中的函数。
你的返回结果仅包含该函数，不需要任何注释或者解释，返回格式如下：
```kotlin
suspend fun llmGeneratedFunction(driver: WebDriver) {
    // 你的代码
}
```

请注意：
1. 如果需要选择页面元素，请使用提供的交互元素列表中的 selector
2. 对于点击操作，优先选择按钮或链接元素
3. 对于输入操作，优先选择输入框元素
4. 如果没有合适的交互元素，请生成一个空的挂起函数
        """.trimIndent()

        val PULSAR_SESSION_SOURCE_CODE_USE_MESSAGE_TEMPLATE = """
以下是抓取网页的 API 接口及其注释，你可以使用这些接口来抓取网页。

{{pulsarSessionSourceCode}}

PulsarSession 代码结束。

如果用户要求你抓取，你需要生成一个函数实现用户的需求。
该函数接收一个 PulsarSession 对象，你仅允许使用 PulsarSession 接口中的函数。
你的返回结果仅包含该函数，不需要任何注释或者解释，返回格式如下：
```kotlin
suspend fun llmGeneratedFunction(session: PulsarSession) {
    // 你的代码
}
```
        """.trimIndent()

        private val extractElementsScript = """
    // JavaScript code to extract interactive elements from the page
    (function() {
        return Array.from(document.querySelectorAll('*')).map(el => ({
            id: el.id,
            tagName: el.tagName,
            selector: el.tagName.toLowerCase() + (el.id ? ('#' + el.id) : ''),
            text: el.innerText || '',
            type: el.type || null,
            href: el.href || null,
            className: el.className || null,
            placeholder: el.placeholder || null,
            value: el.value || null,
            isVisible: !!(el.offsetWidth || el.offsetHeight || el.getClientRects().length),
            bounds: el.getBoundingClientRect()
        }));
    })();
""".trimIndent()
    }
}
