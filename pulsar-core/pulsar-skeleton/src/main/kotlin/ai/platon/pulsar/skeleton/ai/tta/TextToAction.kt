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

    // Tool-use helpers -------------------------------------------------------------------------
    internal data class ToolCall(val name: String, val args: Map<String, Any?>)

    private fun buildToolUsePrompt(userPrompt: String) = """
你现在以工具调用模式工作。给定用户指令, 只返回可执行的 WebDriver 工具调用 JSON。
工具列表(函数与参数说明):
- click(selector: String)
- fill(selector: String, text: String)
- navigateTo(url: String)
- scrollDown(count: Int = 1)
- scrollUp(count: Int = 1)
- scrollToMiddle(ratio: Double = 0.5)
- waitForSelector(selector: String, timeoutMillis: Long = 5000)
- check(selector: String)
- uncheck(selector: String)

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

用户指令: $userPrompt
""".trimIndent()

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

    fun useWebDriver(prompt: String, driver: WebDriver): ActionDescription {
        TODO("generate EXACT ONE WebDriver Action With Interactive Elements")
    }

    fun generateWebDriverActionsWithToolCallSpecs(prompt: String): ActionDescription {
        val toolPrompt = buildToolUsePrompt(prompt)
        val response = model?.call(toolPrompt) ?: ModelResponse.LLM_NOT_AVAILABLE
        val toolCalls = parseToolCalls(response.content)
        val functionCalls = if (toolCalls.isNotEmpty()) {
            toolCalls.mapNotNull { toolCallToDriverLine(it) }
        } else {
            response.content.split("\n").map { it.trim() }.filter { it.startsWith("driver.") && it.contains("(") }
        }
        return ActionDescription(functionCalls, null, response)
    }

    fun generateWebDriverActionsWithSourceCode(prompt: String): ModelResponse {
        val promptWithSystemMessage = """
            $webDriverSourceCodeUseMessage
            
            $prompt
            
        """.trimIndent()

        return model?.call(promptWithSystemMessage) ?: ModelResponse.LLM_NOT_AVAILABLE
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
    suspend fun extractInteractiveElements(driver: WebDriver): List<InteractiveElement> {
        val result = driver.evaluate(extractElementsScript)
        return parseInteractiveElements(result)
    }

    private fun extractFunctionCalls(content: String): List<String> = content.split('\n')
        .map { it.trim() }
        .filter { it.startsWith("driver.") && it.contains('(') && !it.startsWith("//") }

    private fun selectBestMatchingElement(command: String, elements: List<InteractiveElement>): InteractiveElement? {
        val norm = command.lowercase()
        // prioritized exact text/placeholder/type clues
        elements.forEach { e ->
            val txt = e.text.lowercase()
            val ph = e.placeholder?.lowercase() ?: ""
            val type = e.type?.lowercase() ?: ""
            if (txt.isNotBlank() && norm.contains(txt)) return e
            if (ph.isNotBlank() && norm.contains(ph)) return e
            if (norm.contains("search") && (type == "search" || ph.contains("search") || txt.contains("search"))) return e
            if (norm.contains("submit") && (type == "submit" || txt.contains("submit") || txt.contains("提交"))) return e
        }
        // fallback visible element
        return elements.firstOrNull { it.isVisible } ?: elements.firstOrNull()
    }

    /**
     * Load JavaScript script from resource file
     */
    private fun loadResourceScript(resourcePath: String): String {
        return try {
            this::class.java.getResourceAsStream(resourcePath)?.use { inputStream ->
                inputStream.bufferedReader().use { it.readText() }
            } ?: throw IllegalArgumentException("Resource not found: $resourcePath")
        } catch (e: Exception) {
            throw RuntimeException("Failed to load JavaScript resource: $resourcePath", e)
        }
    }

    // WebDriver method information for two-stage optimization
    private val webDriverMethods: Map<String, WebDriverMethodInfo> by lazy {
        initializeWebDriverMethods()
    }

    /**
     * Initialize WebDriver methods information for two-stage optimization
     * Now dynamically loads from MiniWebDriver interface using reflection
     */
    private fun initializeWebDriverMethods(): Map<String, WebDriverMethodInfo> {
        val methods = mutableMapOf<String, WebDriverMethodInfo>()

        try {
            // Use reflection to get all methods from MiniWebDriver interface
            val webDriverClass = Class.forName("ai.platon.pulsar.skeleton.crawl.fetch.driver.MiniWebDriver")

            // Get all declared methods from the interface
            val declaredMethods = webDriverClass.declaredMethods

            for (method in declaredMethods) {
                // Skip synthetic and bridge methods
                if (method.isSynthetic || method.isBridge) continue

                // Generate method signature
                val methodSignature = generateMethodSignature(method)

                // Extract method description from source code or use default
                val description = extractMethodDescription(method) ?: "Execute ${method.name} operation"

                methods[methodSignature] = WebDriverMethodInfo(
                    methodSignature,
                    description
                )
            }

        } catch (e: Exception) {
            // Fallback to predefined methods if reflection fails
            return initializeFallbackWebDriverMethods()
        }

        return methods
    }

    /**
     * Generate method signature string from reflection Method object
     */
    private fun generateMethodSignature(method: java.lang.reflect.Method): String {
        val name = method.name
        val paramTypes = method.parameterTypes
        val params = mutableListOf<String>()

        // Map common parameter names based on method patterns
        for (i in paramTypes.indices) {
            val paramType = paramTypes[i]
            val paramName = when {
                paramType == String::class.java && name.contains("selector", ignoreCase = true) -> "selector: String"
                paramType == String::class.java && name.contains("url", ignoreCase = true) -> "url: String"
                paramType == String::class.java && name.contains("text", ignoreCase = true) -> "text: String"
                paramType == String::class.java && name.contains("pattern", ignoreCase = true) -> "pattern: String"
                paramType == String::class.java && name.contains("key", ignoreCase = true) -> "key: String"
                paramType == String::class.java && name.contains("name", ignoreCase = true) -> "name: String"
                paramType == String::class.java && name.contains("domain", ignoreCase = true) -> "domain: String?"
                paramType == String::class.java && name.contains("path", ignoreCase = true) -> "path: String?"
                paramType == String::class.java && name.contains("attr", ignoreCase = true) -> "attrName: String"
                paramType == String::class.java && name.contains("root", ignoreCase = true) -> "rootSelector: String"
                paramType == String::class.java -> "param${i}: String"
                paramType == Int::class.java && name.contains("count", ignoreCase = true) -> "count: Int = 1"
                paramType == Int::class.java -> "n: Int"
                paramType == Long::class.java && name.contains("timeout", ignoreCase = true) -> "timeoutMillis: Long"
                paramType == Long::class.java -> "value: Long"
                paramType == Double::class.java && name.contains("ratio", ignoreCase = true) -> "ratio: Double"
                paramType == Double::class.java -> "value: Double"
                paramType == Boolean::class.java -> "flag: Boolean"
                else -> "param${i}: ${paramType.simpleName}"
            }
            params.add(paramName)
        }

        return "$name(${params.joinToString(", ")})"
    }

    /**
     * Extract method description from webDriverSourceCode or use pattern matching
     */
    private fun extractMethodDescription(method: java.lang.reflect.Method): String? {
        val methodName = method.name
        val methodPattern = """fun\s+$methodName\s*\([^)]*\)[^{]*""".toRegex()
        val match = methodPattern.find(webDriverSourceCode)
        if (match != null) {
            val methodIndex = webDriverSourceCode.indexOf(match.value)
            val beforeMethod = webDriverSourceCode.substring(0, methodIndex)
            // Correct KDoc block pattern
            val docPattern = """/\*\*([^*]|\*(?!/))*\*/""".toRegex()
            val docMatches = docPattern.findAll(beforeMethod).toList()
            if (docMatches.isNotEmpty()) {
                val lastDoc = docMatches.last().value
                return extractDescriptionFromComment(lastDoc)
            }
        }
        return generateDescriptionFromMethodName(methodName)
    }

    /**
     * Extract meaningful description from KDoc comment
     */
    private fun extractDescriptionFromComment(docComment: String): String {
        // Remove /** */ and clean up
        val cleaned = docComment
            .removePrefix("/**")
            .removeSuffix("*/")
            .split("\n")
            .map { it.trim().removePrefix("*").trim() }
            .filter { it.isNotEmpty() && !it.startsWith("@") }
            .joinToString(" ")
            .trim()

        return if (cleaned.isNotEmpty()) cleaned else "WebDriver operation"
    }

    /**
     * Generate description based on method name patterns
     */
    private fun generateDescriptionFromMethodName(methodName: String): String {
        return when {
            methodName.startsWith("navigate") -> "Navigate page to the specified URL"
            methodName.startsWith("click") && methodName.contains("Text") -> "Click on element(s) where text content matches the pattern"
            methodName.startsWith("click") && methodName.contains("Matches") -> "Click on element(s) where attribute value matches the pattern"
            methodName.startsWith("click") && methodName.contains("Nth") -> "Click the nth element within the specified selector"
            methodName.startsWith("click") -> "Click on element(s) matching the CSS selector"
            methodName.startsWith("fill") -> "Fill text into input element matching the selector"
            methodName.startsWith("type") -> "Type text into element matching the selector"
            methodName.startsWith("press") -> "Press a key on element matching the selector"
            methodName.startsWith("check") -> "Check checkbox or radio button matching the selector"
            methodName.startsWith("uncheck") -> "Uncheck checkbox matching the selector"
            methodName.startsWith("scroll") && methodName.contains("Down") -> "Scroll down by specified number of times"
            methodName.startsWith("scroll") && methodName.contains("Up") -> "Scroll up by specified number of times"
            methodName.startsWith("scroll") && methodName.contains("Top") -> "Scroll to the top of the page"
            methodName.startsWith("scroll") && methodName.contains("Bottom") -> "Scroll to the bottom of the page"
            methodName.startsWith("scroll") && methodName.contains("Middle") -> "Scroll to specified ratio of the page"
            methodName.startsWith("scroll") -> "Scroll to element matching the selector"
            methodName.startsWith("waitFor") -> "Wait for element matching selector to become present"
            methodName.startsWith("exists") -> "Check if element matching selector exists"
            methodName.startsWith("isVisible") || methodName == "visible" -> "Check if element matching selector is visible"
            methodName.startsWith("isHidden") -> "Check if element matching selector is hidden"
            methodName.startsWith("isChecked") -> "Check if checkbox/radio button matching selector is checked"
            methodName.startsWith("focus") -> "Focus on element matching the selector"
            methodName.startsWith("select") && methodName.contains("Text") -> "Get text content of element(s) matching selector"
            methodName.startsWith("select") && methodName.contains("Attribute") -> "Get attribute value of element(s) matching selector"
            methodName.startsWith("select") -> "Select element(s) matching the selector"
            methodName.contains("url") -> "Get or manipulate URL"
            methodName.contains("Cookie") -> "Manage browser cookies"
            else -> "Execute $methodName operation"
        }
    }

    /**
     * Fallback method initialization if reflection fails
     */
    private fun initializeFallbackWebDriverMethods(): Map<String, WebDriverMethodInfo> {
        val methods = mutableMapOf<String, WebDriverMethodInfo>()

        // Keep essential methods as fallback
        methods["click(selector: String, count: Int = 1)"] = WebDriverMethodInfo(
            "click(selector: String, count: Int = 1)",
            "Click on element(s) matching the CSS selector. Can specify number of clicks."
        )

        methods["fill(selector: String, text: String)"] = WebDriverMethodInfo(
            "fill(selector: String, text: String)",
            "Fill text into input element matching the selector"
        )

        methods["navigateTo(url: String)"] = WebDriverMethodInfo(
            "navigateTo(url: String)",
            "Navigate page to the specified URL"
        )

        methods["scrollDown(count: Int = 1)"] = WebDriverMethodInfo(
            "scrollDown(count: Int = 1)",
            "Scroll down by specified number of times"
        )

        methods["waitForSelector(selector: String, timeoutMillis: Long)"] = WebDriverMethodInfo(
            "waitForSelector(selector: String, timeoutMillis: Long)",
            "Wait for element matching selector to become present"
        )

        return methods
    }

    /**
     * Stage 1: Select top candidate methods based on compact method list
     */
    private fun selectMethodCandidates(prompt: String): MethodSelectionResult {
        val compactMethodList = webDriverMethods.keys.joinToString("\n") { "- $it" }

        val selectionPrompt = """
请根据用户指令，从以下WebDriver方法列表中选择5个最相关的候选方法：

可用方法列表：
$compactMethodList

用户指令：$prompt

请仅返回方法名列表，每行一个，格式如下：
- click(selector: String, count: Int = 1)
- fill(selector: String, text: String)
...

最多选择5个最相关的方法。
        """.trimIndent()

        val response = model?.call(selectionPrompt) ?: ModelResponse.LLM_NOT_AVAILABLE

        val selectedMethods = response.content.split("\n")
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .filter { it.startsWith("- ") }
            .take(5)
            .toList()

        return MethodSelectionResult(selectedMethods, response)
    }

    /**
     * Stage 2: Generate detailed function using selected methods
     */
    private fun generateDetailedFunction(
        prompt: String,
        selectedMethods: List<String>,
        interactiveElements: List<InteractiveElement>
    ): ModelResponse {
        val detailedMethodInfo = selectedMethods.mapNotNull { methodSignature ->
            webDriverMethods[methodSignature]?.let { info ->
                """
方法：${info.methodSignature}
说明：${info.fullDescription}
                """.trimIndent()
            }
        }.joinToString("\n\n")

        val elementsDescription = interactiveElements.take(10)
            .mapIndexed { index, element ->
                "$index: ${element.description}"
            }
            .joinToString("\n")

        val detailedPrompt = """
你可以使用以下WebDriver方法来实现用户需求：

$detailedMethodInfo

页面可交互元素：
$elementsDescription

用户指令：$prompt

请生成一个kotlin函数来实现用户需求，格式如下：
```kotlin
suspend fun llmGeneratedFunction(driver: WebDriver) {
    // 你的代码
}
```

注意事项：
1. 只使用提供的方法
2. 优先选择最匹配的页面元素
3. 确保生成的代码语法正确
4. 如果无法实现需求，生成空函数
        """.trimIndent()

        return model?.call(detailedPrompt) ?: ModelResponse.LLM_NOT_AVAILABLE
    }

    /**
     * Optimized two-stage WebDriver action generation
     * Stage 1: AI selects 5 best candidate methods from compact list
     * Stage 2: AI generates detailed function using full method descriptions
     */
    suspend fun generateWebDriverActionWithInteractiveElements(prompt: String, driver: WebDriver?): ActionDescription {
        if (driver == null) {
            val emptyResponse = ModelResponse("""
                suspend fun llmGeneratedFunction(driver: WebDriver) {
                    // No WebDriver instance available
                }
            """.trimIndent(), ResponseState.OTHER)
            return ActionDescription(emptyList(), null, emptyResponse)
        }

        try {
            val interactiveElements = extractInteractiveElements(driver)

            if (interactiveElements.isEmpty()) {
                val emptyResponse = ModelResponse("""
                    suspend fun llmGeneratedFunction(driver: WebDriver) {
                        // No interactive elements found on the page
                    }
                """.trimIndent(), ResponseState.OTHER)
                return ActionDescription(emptyList(), null, emptyResponse)
            }

            // Stage 1: Select candidate methods (reduced prompt size)
            val candidateSelection = selectMethodCandidates(prompt)

            if (candidateSelection.selectedMethods.isEmpty()) {
                val emptyResponse = ModelResponse("""
                    suspend fun llmGeneratedFunction(driver: WebDriver) {
                        // No suitable methods found for the request
                    }
                """.trimIndent(), ResponseState.OTHER)
                return ActionDescription(emptyList(), null, emptyResponse)
            }

            // Stage 2: Generate detailed function with full descriptions of selected methods
            val detailedResponse = generateDetailedFunction(prompt, candidateSelection.selectedMethods, interactiveElements)

            val functionCalls = extractFunctionCalls(detailedResponse.content)
            val selectedElement = selectBestMatchingElement(prompt, interactiveElements)

            return ActionDescription(functionCalls, selectedElement, detailedResponse)
        } catch (e: Exception) {
            val errorResponse = ModelResponse("""
                suspend fun llmGeneratedFunction(driver: WebDriver) {
                    // Error occurred during optimization: ${e.message}
                }
            """.trimIndent(), ResponseState.OTHER)
            return ActionDescription(emptyList(), null, errorResponse)
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
""".trimIndent()
    }
}
