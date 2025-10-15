package ai.platon.pulsar.skeleton.ai.tta

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.ai.llm.PromptTemplate
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.external.BrowserChatModel
import ai.platon.pulsar.external.ChatModelFactory
import ai.platon.pulsar.external.ModelResponse
import ai.platon.pulsar.external.ResponseState
import ai.platon.pulsar.skeleton.ai.ActionDescription
import ai.platon.pulsar.skeleton.ai.detail.ElementBounds
import ai.platon.pulsar.skeleton.ai.detail.InteractiveElement
import ai.platon.pulsar.skeleton.common.llm.LLMUtils
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.runBlocking
import org.apache.hadoop.record.compiler.generated.Rcc.driver
import java.nio.file.Files

open class TextToAction(val conf: ImmutableConfig) {
    private val logger = getLogger(this)

    val baseDir = AppPaths.get("tta")

    val chatModel: BrowserChatModel = ChatModelFactory.getOrCreate(conf)
    val webDriverSourceCodeFile = baseDir.resolve("MiniWebDriver.kt")
    var webDriverSourceCode: String
        private set
    var webDriverSourceCodeUseMessage: String
        private set

    // Tool-use helpers -------------------------------------------------------------------------
    internal data class ToolCall(val name: String, val args: Map<String, Any?>)

    init {
        Files.createDirectories(baseDir)

        LLMUtils.copyWebDriverFile(webDriverSourceCodeFile)
        webDriverSourceCode = Files.readAllLines(webDriverSourceCodeFile)
            .filter { it.contains(" fun ") }
            .joinToString("\n")
        webDriverSourceCodeUseMessage = PromptTemplate(WEB_DRIVER_SOURCE_CODE_USE_MESSAGE_TEMPLATE)
            .render(mapOf("webDriverSourceCode" to webDriverSourceCode))
    }

    open fun generateWebDriverAction(
        instruction: String,
        driver: WebDriver,
        screenshotB64: String? = null,
    ): ActionDescription {
        return runBlocking { generateWebDriverActionDeferred(instruction, driver, screenshotB64) }
    }

    /**
     * Generate EXACT ONE WebDriver action with interactive elements.
     *
     * @param instruction The instruction
     * @param driver The driver to use to collect the context, such as interactive elements
     * @return The action description
     * */
    open suspend fun generateWebDriverActionDeferred(
        instruction: String,
        driver: WebDriver,
        screenshotB64: String? = null,
    ): ActionDescription {
        try {
            val interactiveElements = extractInteractiveElements(driver)

            return generateWebDriverActionDeffered(instruction, interactiveElements, screenshotB64)
        } catch (e: Exception) {
            val errorResponse = ModelResponse(
                """
                suspend fun llmGeneratedFunction(driver: WebDriver) {
                    // Error occurred during optimization: ${e.message}
                }
            """.trimIndent(), ResponseState.OTHER
            )
            return ActionDescription(emptyList(), null, errorResponse)
        }
    }

    /**
     * Generate EXACT ONE WebDriver action with interactive elements.
     *
     * @param instruction The action description with plain text
     * @param driver The driver to use to collect the context, such as interactive elements
     * @return The action description
     * */
    open fun generateWebDriverAction(
        instruction: String,
        interactiveElements: List<InteractiveElement> = listOf(),
        screenshotB64: String? = null
    ): ActionDescription {
        return runBlocking {
            generateWebDriverActionsWithToolCallSpecsDeferred(instruction, interactiveElements, screenshotB64, 1)
        }
    }

    open suspend fun generateWebDriverActionDeffered(
        instruction: String,
        interactiveElements: List<InteractiveElement> = listOf(),
        screenshotB64: String? = null
    ): ActionDescription {
        return generateWebDriverActionsWithToolCallSpecsDeferred(instruction, interactiveElements, screenshotB64, 1)
    }

    open fun generateWebDriverActionsWithToolCallSpecs(
        instruction: String,
        interactiveElements: List<InteractiveElement> = listOf(),
        screenshotB64: String? = null,
        toolCallLimit: Int = 100,
    ): ActionDescription {
        return runBlocking { generateWebDriverActionsWithToolCallSpecsDeferred(instruction, interactiveElements, screenshotB64, toolCallLimit) }
    }

    open suspend fun generateWebDriverActionsWithToolCallSpecsDeferred(
        instruction: String,
        interactiveElements: List<InteractiveElement> = listOf(),
        screenshotB64: String? = null,
        toolCallLimit: Int = 100,
    ): ActionDescription {

        val systemPrompt = when {
            instruction.contains(AGENT_SYSTEM_PROMPT_PREFIX_20) -> instruction
            else -> buildOperatorSystemPrompt(instruction)
        }
        val toolUsePrompt = buildToolUsePrompt(systemPrompt, interactiveElements, toolCallLimit)
        val response = if (screenshotB64 != null) {
            chatModel.callUmSm(toolUsePrompt, "", null, screenshotB64, "image/jpeg")
        } else {
            chatModel.call(toolUsePrompt)
        }

        return modelResponseToActionDescription(response, toolCallLimit)
    }

    open fun generateWebDriverActionsWithSourceCode(
        command: String,
        interactiveElements: List<InteractiveElement> = listOf()
    ): ModelResponse {
        return runBlocking { generateWebDriverActionsWithSourceCodeDeferred(command, interactiveElements) }
    }

    open suspend fun generateWebDriverActionsWithSourceCodeDeferred(
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

        return chatModel.call(prompt)
    }

    fun buildToolUsePrompt(
        systemPrompt: String,
        interactiveElements: List<InteractiveElement>,
        toolCallLimit: Int = 100,
    ): String {
        val prompt = buildString {
            append(systemPrompt)

            appendLine("每次最多调用 $toolCallLimit 个工具")

            if (interactiveElements.isNotEmpty()) {
                appendLine("可交互元素列表: ")
                interactiveElements.forEach { e -> appendLine(e.toString()) }
            }

            appendLine()
        }

        return prompt
    }

    fun buildOperatorSystemPrompt(goal: String): String {
        return """
$AGENT_SYSTEM_PROMPT

用户总目标：$goal
        """.trimIndent()
    }

    fun formatInteractiveElements(elements: List<InteractiveElement>, limit: Int = 200, charLimitPerLine: Int = 180): String {
        if (elements.isEmpty()) return "(无)"
        val ranked = rankInteractiveElements(elements).take(limit)
        return ranked.mapIndexed { idx, e ->
            val base = e.description
            val clipped = if (base.length > charLimitPerLine) base.take(charLimitPerLine - 3) + "..." else base
            "${idx + 1}. $clipped"
        }.joinToString("\n")
    }

    fun rankInteractiveElements(elements: List<InteractiveElement>): List<InteractiveElement> {
        // Simple heuristic scoring: prioritize buttons, inputs (empty), anchors with text, then others
        return elements.sortedWith(compareByDescending<InteractiveElement> { e ->
            when (e.tagName.lowercase()) {
                "button" -> 5
                "input" -> if (e.value.isNullOrBlank()) 4 else 3
                "select" -> 3
                "textarea" -> 3
                "a" -> if (e.text.isNotBlank()) 2 else 1
                else -> 0
            }
        }.thenByDescending { it.text.length }.thenBy { it.selector.length })
    }

    protected fun modelResponseToActionDescription(response: ModelResponse, toolCallLimit: Int = 1): ActionDescription {
        val toolCalls = parseToolCalls(response.content).take(toolCallLimit)
        val functionCalls = if (toolCalls.isNotEmpty()) {
            toolCalls.mapNotNull { toolCallToDriverLine(it) }
        } else {
            response.content.split("\n").map { it.trim() }.filter { it.startsWith("driver.") && it.contains("(") }
        }
        return ActionDescription(functionCalls, null, response)
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
        // Navigation
        "navigateTo" -> tc.args["url"]?.let { "driver.navigateTo(\"$it\")" }
        // Backward compatibility for older prompts
        "goto" -> tc.args["url"]?.let { "driver.navigateTo(\"$it\")" }
        // Wait
        "waitForSelector" -> tc.args["selector"]?.let { sel -> "driver.waitForSelector(\"$sel\", ${(tc.args["timeoutMillis"] ?: 5000)}L)" }
        // Status checking (first batch of new tools)
        "exists" -> tc.args["selector"]?.let { "driver.exists(\"$it\")" }
        "isVisible" -> tc.args["selector"]?.let { "driver.isVisible(\"$it\")" }
        "focus" -> tc.args["selector"]?.let { "driver.focus(\"$it\")" }
        // Basic interactions
        "click" -> tc.args["selector"]?.let { "driver.click(\"$it\")" }
        "fill" -> tc.args["selector"]?.let { s -> "driver.fill(\"$s\", \"${tc.args["text"] ?: ""}\")" }
        "press" -> tc.args["selector"]?.let { s -> tc.args["key"]?.let { k -> "driver.press(\"$s\", \"$k\")" } }
        "check" -> tc.args["selector"]?.let { "driver.check(\"$it\")" }
        "uncheck" -> tc.args["selector"]?.let { "driver.uncheck(\"$it\")" }
        // Scrolling
        "scrollDown" -> "driver.scrollDown(${tc.args["count"] ?: 1})"
        "scrollUp" -> "driver.scrollUp(${tc.args["count"] ?: 1})"
        "scrollTo" -> tc.args["selector"]?.let { "driver.scrollTo(\"$it\")" }
        "scrollToTop" -> "driver.scrollToTop()"
        "scrollToBottom" -> "driver.scrollToBottom()"
        "scrollToMiddle" -> "driver.scrollToMiddle(${tc.args["ratio"] ?: 0.5})"
        "scrollToScreen" -> tc.args["screenNumber"]?.let { n -> "driver.scrollToScreen(${n})" }
        // Advanced clicks
        "clickTextMatches" -> tc.args["selector"]?.let { s ->
            val pattern = tc.args["pattern"] ?: return@let null
            val count = tc.args["count"] ?: 1
            "driver.clickTextMatches(\"$s\", \"$pattern\", $count)"
        }
        "clickMatches" -> tc.args["selector"]?.let { s ->
            val attr = tc.args["attrName"] ?: return@let null
            val pattern = tc.args["pattern"] ?: return@let null
            val count = tc.args["count"] ?: 1
            "driver.clickMatches(\"$s\", \"$attr\", \"$pattern\", $count)"
        }
        "clickNthAnchor" -> tc.args["n"]?.let { n ->
            val root = tc.args["rootSelector"]?.toString() ?: "body"
            "driver.clickNthAnchor(${n}, \"$root\")"
        }
        // Enhanced navigation
        "waitForNavigation" -> {
            val oldUrl = tc.args["oldUrl"]?.toString() ?: ""
            val timeout = tc.args["timeoutMillis"] ?: 5000L
            "driver.waitForNavigation(\"$oldUrl\", ${timeout}L)"
        }
        "goBack" -> "driver.goBack()"
        "goForward" -> "driver.goForward()"
        // Screenshots
        "captureScreenshot" -> {
            val sel = tc.args["selector"]?.toString()
            if (sel.isNullOrBlank()) "driver.captureScreenshot()" else "driver.captureScreenshot(\"$sel\")"
        }
        // Timing
        "delay" -> "driver.delay(${tc.args["millis"] ?: 1000}L)"
        else -> null
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

        // Support strings like: JsEvaluation(value=[ {...}, {...} ], unserializableValue=null, ...)
        val normalized = when {
            jsonString.trimStart().startsWith("[") || jsonString.trimStart().startsWith("{") -> jsonString
            jsonString.contains("JsEvaluation(") -> {
                // Try to extract the substring that starts from the first '[' and ends at the matching ']'
                val start = jsonString.indexOf('[')
                val end = jsonString.lastIndexOf(']')
                if (start >= 0 && end > start) jsonString.substring(start, end + 1) else jsonString
            }
            else -> jsonString
        }

        return try {
            val root = JsonParser.parseString(normalized)
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
                    x = boundsObj?.get("x")?.takeIf { it.isJsonPrimitive }?.asDouble ?: 0.0,
                    y = boundsObj?.get("y")?.takeIf { it.isJsonPrimitive }?.asDouble ?: 0.0,
                    width = boundsObj?.get("width")?.takeIf { it.isJsonPrimitive }?.asDouble ?: 0.0,
                    height = boundsObj?.get("height")?.takeIf { it.isJsonPrimitive }?.asDouble ?: 0.0
                )
                InteractiveElement(
                    id = obj.get("id")?.takeIf { it.isJsonPrimitive }?.asString ?: "",
                    tagName = obj.get("tagName")?.takeIf { it.isJsonPrimitive }?.asString ?: "",
                    selector = obj.get("selector")?.takeIf { it.isJsonPrimitive }?.asString ?: "",
                    text = obj.get("text")?.takeIf { it.isJsonPrimitive }?.asString?.take(100) ?: "",
                    type = obj.get("type")?.takeIf { it.isJsonPrimitive }?.asString,
                    href = obj.get("href")?.takeIf { it.isJsonPrimitive }?.asString,
                    className = obj.get("className")?.takeIf { it.isJsonPrimitive }?.asString,
                    placeholder = obj.get("placeholder")?.takeIf { it.isJsonPrimitive }?.asString,
                    value = obj.get("value")?.takeIf { it.isJsonPrimitive }?.asString,
                    isVisible = obj.get("isVisible")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: false,
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
    /**
     * deprecated, use DomService to build interactive element instead
     * */
    @Deprecated("use DomService to build interactive element instead")
    fun extractInteractiveElements(driver: WebDriver): List<InteractiveElement> {
        return runBlocking { extractInteractiveElementsDeferred(driver) }
    }

    /**
     * deprecated, use DomService to build interactive element instead
     * */
    @Deprecated("use DomService to build interactive element instead")
    suspend fun extractInteractiveElementsDeferred(driver: WebDriver): List<InteractiveElement> {
        try {
            // If you want to execute a function, convert it to IIFE (Immediately Invoked Function Expression).
            val result = driver.evaluateDetail(EXTRACT_INTERACTIVE_ELEMENTS_EXPRESSION)

            // NOTE: Do NOT coerce to string here; parseInteractiveElements can handle List/Map/JSON string
            val elements = parseInteractiveElements(result?.value)
            // Kotlin-side safety filter: only visible interactive controls
            val filtered = elements.filter { e ->
                val tag = e.tagName.lowercase()
                e.isVisible &&
                        (tag == "input" || tag == "select" || tag == "textarea" || tag == "button"
                                || (tag == "a" && !e.href.isNullOrBlank()))
            }

            // Deduplicate by selector to avoid duplicates from complex pages
            return filtered.distinctBy { it.selector }
        } catch (e: Exception) {
            return emptyList()
        }
    }

    companion object {

        const val TOOL_CALL_LIST = """
- navigateTo(url: String)
- waitForSelector(selector: String, timeoutMillis: Long = 5000)
- exists(selector: String): Boolean
- isVisible(selector: String): Boolean
- focus(selector: String)
- click(selector: String)
- fill(selector: String, text: String)
- press(selector: String, key: String)
- check(selector: String)
- uncheck(selector: String)
- scrollDown(count: Int = 1)
- scrollUp(count: Int = 1)
- scrollTo(selector: String)
- scrollToTop()
- scrollToBottom()
- scrollToMiddle(ratio: Double = 0.5)
- scrollToScreen(screenNumber: Double)
- clickTextMatches(selector: String, pattern: String, count: Int = 1)
- clickMatches(selector: String, attrName: String, pattern: String, count: Int = 1)
- clickNthAnchor(n: Int, rootSelector: String = "body")
- waitForNavigation(oldUrl: String = "", timeoutMillis: Long = 5000): Long
- goBack()
- goForward()
- captureScreenshot()
- captureScreenshot(selector: String)
- delay(millis: Long)
- stop()
    """

        val AGENT_SYSTEM_PROMPT = """
你是一个网页通用代理，目标是基于用户目标一步一步完成任务。
重要指南：
1) 将复杂动作拆成原子步骤；
2) 一次仅做一个动作（如：单击一次、输入一次、选择一次）；
3) 不要在一步中合并多个动作；
4) 多个动作用多步表达；
5) 始终验证目标元素存在且可见后再执行操作；
6) 遇到错误时尝试替代方案或优雅终止；

输出严格使用 JSON 字段：

{
  tool_calls: [ { name: string, args: object } ]
  taskComplete: boolean
}

安全要求：
- 仅操作可见的交互元素
- 避免快速连续操作，适当等待页面加载
- 遇到验证码或安全提示时停止执行

工具规范：
$TOOL_CALL_LIST

        """.trimIndent()

        val AGENT_SYSTEM_PROMPT_PREFIX_20 = AGENT_SYSTEM_PROMPT.take(20)

        const val WEB_DRIVER_SOURCE_CODE_USE_MESSAGE_TEMPLATE = """
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
        """

        const val PULSAR_SESSION_SOURCE_CODE_USE_MESSAGE_TEMPLATE = """
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
        """

        // JavaScript code to extract truly interactive elements from the page
        val EXTRACT_INTERACTIVE_ELEMENTS_EXPRESSION = """
    (function() {
        // Basic CSS.escape polyfill
        if (typeof window.CSS === 'undefined') { window.CSS = {}; }
        if (!window.CSS.escape) {
            window.CSS.escape = function(value) {
                if (value == null) return '';
                return String(value).replace(/([#.:\[\]"'\\\s>+~])/g, '\\$1');
            };
        }

        const isVisible = (el) => {
            const style = window.getComputedStyle(el);
            if (style.display === 'none' || style.visibility === 'hidden' || style.opacity === '0') return false;
            const rect = el.getBoundingClientRect();
            return rect.width > 0 && rect.height > 0;
        };

        const isDisabled = (el) => el.hasAttribute('disabled') || el.getAttribute('aria-disabled') === 'true';

        const buildSelector = (el) => {
            const tag = el.tagName.toLowerCase();
            if (el.id) return '#' + CSS.escape(el.id);
            const testId = el.getAttribute('data-testid');
            if (testId) return `[data-testid="${'$'}{CSS.escape(testId)}"]`;
            const nameAttr = el.getAttribute('name');
            if (nameAttr) return `${'$'}{tag}[name="${'$'}{CSS.escape(nameAttr)}"]`;
            if (el.hasAttribute('onclick')) {
                const oc = el.getAttribute('onclick').replace(/"/g, '\\"');
                return `${'$'}{tag}[onclick="${'$'}{oc}"]`;
            }
            if (tag === 'a' && el.getAttribute('href')) {
                const href = el.getAttribute('href').replace(/"/g, '\\"');
                return `a[href="${'$'}{href}"]`;
            }
            const typeAttr = el.getAttribute('type');
            if (typeAttr) return `${'$'}{tag}[type="${'$'}{CSS.escape(typeAttr)}"]`;
            // Fallback to nth-of-type path
            let path = tag;
            let node = el;
            while (node && node.parentElement) {
                const parent = node.parentElement;
                if (parent.id) {
                    const idx = Array.from(parent.children).filter(c => c.tagName === node.tagName).indexOf(node) + 1;
                    path = parent.tagName.toLowerCase() + '#' + CSS.escape(parent.id) + ' > ' + node.tagName.toLowerCase() + `:nth-of-type(${ '$' }{idx})`;
                    break;
                } else {
                    const idx = Array.from(parent.children).filter(c => c.tagName === node.tagName).indexOf(node) + 1;
                    path = parent.tagName.toLowerCase() + ' > ' + node.tagName.toLowerCase() + `:nth-of-type(${ '$' }{idx})`;
                }
                node = parent;
            }
            return path;
        };

        const candidates = new Set();
        // Form controls and standard interactive elements
        document.querySelectorAll('input, textarea, select, button, a[href]').forEach(el => candidates.add(el));
        // Elements with explicit interactivity
        document.querySelectorAll('[onclick], [contenteditable="true"], [role="button"], [role="link"]').forEach(el => candidates.add(el));

        const interactiveTags = new Set(['INPUT','TEXTAREA','SELECT','BUTTON','A']);
        const hiddenInputTypes = new Set(['hidden']);

        const elements = [];
        candidates.forEach(el => {
            const tag = el.tagName;
            const type = (el.getAttribute('type') || '').toLowerCase();
            if (!isVisible(el)) return;
            if (isDisabled(el)) return;
            if (tag === 'INPUT' && hiddenInputTypes.has(type)) return;

            // Accept if it's a known interactive tag or has onclick/contenteditable/role
            const hasInteractiveRole = el.hasAttribute('onclick') || el.getAttribute('contenteditable') === 'true' || ['button','link'].includes((el.getAttribute('role')||'').toLowerCase());
            const isStandardInteractive = interactiveTags.has(tag) && (tag !== 'A' || el.hasAttribute('href'));
            if (!hasInteractiveRole && !isStandardInteractive) return;

            const selector = buildSelector(el);
            const bounds = el.getBoundingClientRect();
            elements.push({
                id: el.id || '',
                tagName: tag,
                selector: selector,
                text: (el.innerText || '').trim(),
                type: el.getAttribute('type') || null,
                href: el.getAttribute('href') || null,
                className: el.className || null,
                placeholder: el.getAttribute('placeholder') || null,
                value: (el.value !== undefined ? String(el.value) : null),
                isVisible: true,
                bounds: { x: bounds.x, y: bounds.y, width: bounds.width, height: bounds.height }
            });
        });

        // Deduplicate by selector
        const seen = new Set();
        let finalElements = elements.filter(e => !seen.has(e.selector) && seen.add(e.selector));
        return JSON.stringify(finalElements)
    })();
""".trimIndent()
    }
}
