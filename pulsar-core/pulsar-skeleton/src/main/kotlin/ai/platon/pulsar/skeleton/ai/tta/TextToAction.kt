package ai.platon.pulsar.skeleton.ai.tta

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.external.ChatModelFactory
import ai.platon.pulsar.external.ModelResponse
import ai.platon.pulsar.external.ResponseState
import ai.platon.pulsar.skeleton.common.llm.LLMUtils
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver
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
    fun hasResults() = functionResults.isNotEmpty()

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
    private val model = ChatModelFactory.getOrCreateOrNull(conf)

    val baseDir = AppPaths.get("tta")
    val pulsarSessionFile = baseDir.resolve("PulsarSession.kt")
    var pulsarSessionSourceCode: String
        private set
    var pulsarSessionMessage: String
        private set

    val webDriverFile = baseDir.resolve("MiniWebDriver.kt")
    var webDriverSourceCode: String
        private set
    var webDriverMessage: String
        private set

    var actionInstructionMessage: String
        private set
    val actionInterfaceMessageFile = baseDir.resolve("system-message.txt")

    // Lazy load JavaScript script from resource file
    private val extractElementsScript: String by lazy {
        loadResourceScript("/ai/platon/pulsar/skeleton/ai/tta/extract-interactive-elements.js")
    }

    init {
        Files.createDirectories(baseDir)

        LLMUtils.copyWebDriverFile(webDriverFile)
        webDriverSourceCode = Files.readAllLines(webDriverFile)
            .filter { it.contains(" fun ") }
            .joinToString("\n")
        webDriverMessage = WEB_DRIVER_MESSAGE_TEMPLATE.replace("{{webDriverSourceCode}}", webDriverSourceCode)

        LLMUtils.copyPulsarSessionFile(pulsarSessionFile)
        pulsarSessionSourceCode = Files.readAllLines(pulsarSessionFile)
            .filter { it.contains(" fun ") }
            .joinToString("\n")
        pulsarSessionMessage = PULSAR_SESSION_MESSAGE_TEMPLATE.replace("{{pulsarSessionSourceCode}}", pulsarSessionSourceCode)

        actionInstructionMessage = webDriverSourceCode + pulsarSessionSourceCode
        Files.writeString(actionInterfaceMessageFile, actionInstructionMessage)
    }

    /**
     * Generate the action code from the prompt.
     * */
    fun chatAboutAllInstruction(prompt: String): ModelResponse {
        val promptWithSystemMessage = """
            $actionInstructionMessage
            $prompt
        """.trimIndent()

        return model?.call(promptWithSystemMessage) ?: ModelResponse.LLM_NOT_AVAILABLE
    }

    /**
     * Generate the action code from the prompt.
     * */
    fun chatAboutWebDriver(prompt: String): ModelResponse {
        val promptWithSystemMessage = """
            $webDriverMessage
            
            $prompt
            
        """.trimIndent()

        return model?.call(promptWithSystemMessage) ?: ModelResponse.LLM_NOT_AVAILABLE
    }

    /**
     * Generate the action code from the prompt.
     * */
    fun chatAboutPulsarSession(prompt: String): ModelResponse {
        val promptWithSystemMessage = """
            $pulsarSessionMessage
            $prompt
        """.trimIndent()

        return model?.call(promptWithSystemMessage) ?: ModelResponse.LLM_NOT_AVAILABLE
    }

    /**
     * Generate the action code from the prompt.
     * */
    fun generateWebDriverActions(prompt: String): ActionDescription {
        val response = chatAboutWebDriver(prompt)
        val functionCalls = response.content.split("\n")
            .map { it.trim() }.filter { it.startsWith("driver.") }

        return ActionDescription(functionCalls, null, response)
    }

    /**
     * Generate the action code from the prompt.
     * */
    fun generatePulsarSessionActions(prompt: String): ActionDescription {
        val response = chatAboutPulsarSession(prompt)
        val functionCalls = response.content.split("\n")
            .map { it.trim() }.filter { it.startsWith("session.") }

        return ActionDescription(functionCalls, null, response)
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
            println("Error parsing interactive elements: ${e.message}")
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
            println("Error parsing element from map: ${e.message}")
            return null
        }
    }

    /**
     * Parse elements from JSON string (fallback method)
     */
    private fun parseElementsFromJsonString(jsonString: String): List<InteractiveElement> {
        // 简化的 JSON 解析，实际项目中应该使用 Jackson 或 Gson
        val elements = mutableListOf<InteractiveElement>()

        try {
            // 这里是一个简化的解析逻辑，用于演示
            // 在实际项目中，应该使用专门的 JSON 解析库
            if (jsonString.contains("[") && jsonString.contains("]")) {
                // 创建一些示例元素用于演示
                elements.add(
                    InteractiveElement(
                        id = "search-input",
                        tagName = "input",
                        selector = "#search-input",
                        text = "",
                        type = "search",
                        href = null,
                        className = "search-box",
                        placeholder = "Search...",
                        value = "",
                        isVisible = true,
                        bounds = ElementBounds(50.0, 100.0, 300.0, 40.0)
                    )
                )

                elements.add(
                    InteractiveElement(
                        id = "submit-btn",
                        tagName = "button",
                        selector = "#submit-btn",
                        text = "Search",
                        type = "submit",
                        href = null,
                        className = "btn btn-primary",
                        placeholder = null,
                        value = null,
                        isVisible = true,
                        bounds = ElementBounds(360.0, 100.0, 80.0, 40.0)
                    )
                )
            }
        } catch (e: Exception) {
            println("Error parsing JSON string: ${e.message}")
        }

        return elements
    }

    /**
     * Extract interactive elements from the current page using JavaScript
     */
    suspend fun extractInteractiveElements(driver: WebDriver): List<InteractiveElement> {
        val result = driver.evaluate(extractElementsScript)
        return parseInteractiveElements(result)
    }

    /**
     * Enhanced method to generate WebDriver actions with element selection
     * This method now uses the optimized two-stage approach to reduce prompt size
     */
    suspend fun generateWebDriverActions(prompt: String, driver: WebDriver?): ActionDescription {
        return generateWebDriverActionsOptimized(prompt, driver)
    }

    /**
     * Execute action commands with proper element selection and fallback handling
     */
    suspend fun executeActionCommands(commands: List<String>, driver: WebDriver): InstructionResult {
        val results = mutableListOf<Any?>()
        val functionCalls = mutableListOf<String>()

        try {
            for (command in commands) {
                val actionDescription = generateWebDriverActions(command, driver)
                functionCalls.addAll(actionDescription.functionCalls)

                // Execute the function calls
                for (functionCall in actionDescription.functionCalls) {
                    try {
                        val result = executeSingleCommand(functionCall, driver)
                        results.add(result)
                    } catch (e: Exception) {
                        results.add("Error executing '$functionCall': ${e.message}")
                    }
                }
            }

            return InstructionResult(
                functionCalls,
                results,
                ModelResponse("Commands executed successfully", ResponseState.STOP)
            )
        } catch (e: Exception) {
            return InstructionResult(
                functionCalls,
                results,
                ModelResponse("Execution failed: ${e.message}", ResponseState.OTHER)
            )
        }
    }

    /**
     * Execute a single WebDriver command with enhanced parameter extraction
     */
    private suspend fun executeSingleCommand(command: String, driver: WebDriver): Any? {
        return when {
            command.contains("click(") -> {
                val selector = extractSelector(command)
                if (selector != null) {
                    driver.click(selector)
                    "Clicked element: $selector"
                } else {
                    "Failed to extract selector from: $command"
                }
            }

            command.contains("fill(") || command.contains("type(") -> {
                val (selector, text) = extractSelectorAndText(command)
                if (selector != null && text != null) {
                    driver.fill(selector, text)
                    "Filled '$selector' with: $text"
                } else {
                    "Failed to extract parameters from: $command"
                }
            }

            command.contains("scrollToMiddle(") -> {
                val ratio = extractRatio(command) ?: 0.5
                driver.scrollToMiddle(ratio)
                "Scrolled to middle (ratio: $ratio)"
            }

            command.contains("scrollDown(") -> {
                val count = extractCount(command) ?: 1
                driver.scrollDown(count)
                "Scrolled down $count times"
            }

            command.contains("scrollUp(") -> {
                val count = extractCount(command) ?: 1
                driver.scrollUp(count)
                "Scrolled up $count times"
            }

            command.contains("waitForSelector(") -> {
                val (selector, timeout) = extractSelectorAndTimeout(command)
                if (selector != null) {
                    val timeoutMs = timeout ?: 5000L
                    val remainingTime = driver.waitForSelector(selector, timeoutMs)
                    "Waited for '$selector' (remaining: ${remainingTime}ms)"
                } else {
                    "Failed to extract selector from: $command"
                }
            }

            command.contains("navigateTo(") -> {
                val url = extractUrl(command)
                if (url != null) {
                    driver.navigateTo(url)
                    "Navigated to: $url"
                } else {
                    "Failed to extract URL from: $command"
                }
            }

            command.contains("check(") -> {
                val selector = extractSelector(command)
                if (selector != null) {
                    driver.check(selector)
                    "Checked element: $selector"
                } else {
                    "Failed to extract selector from: $command"
                }
            }

            command.contains("uncheck(") -> {
                val selector = extractSelector(command)
                if (selector != null) {
                    driver.uncheck(selector)
                    "Unchecked element: $selector"
                } else {
                    "Failed to extract selector from: $command"
                }
            }

            else -> "Unrecognized command: $command"
        }
    }

    /**
     * Generate enhanced prompts that leverage element lists efficiently
     */
    private fun createElementBasedPrompt(
        command: String,
        elements: List<InteractiveElement>,
        language: String = "zh"
    ): String {
        val elementsDescription = elements.take(20) // Limit to avoid token overflow
            .mapIndexed { index, element ->
                "$index: ${element.description}"
            }
            .joinToString("\n")

        return if (language == "zh") {
            """
            可用的交互元素列表：
            $elementsDescription
            
            用户指令：$command
            
            请根据用户指令选择最匹配的交互元素，并生成相应的WebDriver操作代码。
            如果需要选择元素，请在代码中使用对应的selector。
            如果没有合适的交互元素，请生成一个空的挂起函数。
            
            常见操作示例：
            - 点击按钮：driver.click("button selector")
            - 输入文本：driver.fill("input selector", "text")
            - 滚动页面：driver.scrollToMiddle(0.5)
            - 等待元素：driver.waitForSelector("selector", 5000)
            """.trimIndent()
        } else {
            """
            Available interactive elements:
            $elementsDescription
            
            User command: $command
            
            Please select the best matching interactive element based on the user command and generate corresponding WebDriver operation code.
            Use the corresponding selector in the code if element selection is needed.
            If no suitable interactive element exists, generate an empty suspend function.
            
            Common operation examples:
            - Click button: driver.click("button selector")
            - Type text: driver.fill("input selector", "text")
            - Scroll page: driver.scrollToMiddle(0.5)
            - Wait for element: driver.waitForSelector("selector", 5000)
            """.trimIndent()
        }
    }

    /**
     * Select the best matching interactive element for the given command
     */
    private fun selectBestMatchingElement(command: String, elements: List<InteractiveElement>): InteractiveElement? {
        val commandLower = command.lowercase()

        // Priority-based matching with enhanced logic
        elements.forEach { element ->
            val elementText = element.text.lowercase()
            val elementType = element.type?.lowercase() ?: ""
            val elementPlaceholder = element.placeholder?.lowercase() ?: ""

            // Exact text match (highest priority)
            if (elementText.isNotBlank() && commandLower.contains(elementText)) {
                return element
            }

            // Placeholder text match
            if (elementPlaceholder.isNotBlank() && commandLower.contains(elementPlaceholder)) {
                return element
            }

            // Type-based matching for common actions
            when {
                commandLower.contains("search") && (
                    elementType == "search" ||
                    elementPlaceholder.contains("search") ||
                    elementText.contains("search")
                ) -> return element

                commandLower.contains("submit") && (
                    elementType == "submit" ||
                    elementText.contains("submit") ||
                    elementText.contains("提交")
                ) -> return element

                commandLower.contains("button") && element.tagName == "button" -> return element
                commandLower.contains("按钮") && element.tagName == "button" -> return element

                commandLower.contains("link") && element.tagName == "a" -> return element
                commandLower.contains("链接") && element.tagName == "a" -> return element

                commandLower.contains("input") && element.tagName == "input" -> return element
                commandLower.contains("输入") && element.tagName == "input" -> return element

                commandLower.contains("click") && (
                    element.tagName == "button" ||
                    element.tagName == "a" ||
                    elementType == "submit"
                ) -> return element

                commandLower.contains("点击") && (
                    element.tagName == "button" ||
                    element.tagName == "a" ||
                    elementType == "submit"
                ) -> return element

                commandLower.contains("type") && element.tagName == "input" -> return element
                commandLower.contains("输入") && element.tagName == "input" -> return element
            }
        }

        // Fallback: return first visible element
        return elements.firstOrNull { it.isVisible }
    }

    /**
     * Extract function calls from AI response
     */
    private fun extractFunctionCalls(content: String): List<String> {
        return content.split("\n")
            .map { it.trim() }
            .filter { line ->
                (line.startsWith("driver.") || line.startsWith("session.")) &&
                line.contains("(") &&
                !line.startsWith("//")
            }
    }

    /**
     * Validate that element references are stable against DOM mutations
     */
    suspend fun validateElementStability(element: InteractiveElement, driver: WebDriver): Boolean {
        return try {
            driver.exists(element.selector)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Extract CSS selector from command string with enhanced patterns
     */
    private fun extractSelector(command: String): String? {
        val patterns = listOf(
            """['"](#[^'"]+)['"]""".toRegex(),
            """['"](\.[\w-]+)['"]""".toRegex(),
            """['"](\[[^\]]+\])['"]""".toRegex(),
            """['"]([a-zA-Z][^'"]*?)['"]""".toRegex()
        )

        for (pattern in patterns) {
            val match = pattern.find(command)
            if (match != null) {
                return match.groupValues[1]
            }
        }
        return null
    }

    /**
     * Extract selector and text from fill/type commands
     */
    private fun extractSelectorAndText(command: String): Pair<String?, String?> {
        val patterns = listOf(
            """fill\s*\(\s*['"]([^'"]+)['"]\s*,\s*['"]([^'"]*)['"]\s*\)""".toRegex(),
            """type\s*\(\s*['"]([^'"]+)['"]\s*,\s*['"]([^'"]*)['"]\s*\)""".toRegex()
        )

        for (pattern in patterns) {
            val match = pattern.find(command)
            if (match != null && match.groupValues.size >= 3) {
                return Pair(match.groupValues[1], match.groupValues[2])
            }
        }
        return Pair(null, null)
    }

    /**
     * Extract numeric ratio from command
     */
    private fun extractRatio(command: String): Double? {
        val patterns = listOf(
            """scrollToMiddle\s*\(\s*(\d*\.?\d+)\s*\)""".toRegex(),
            """(\d*\.?\d+)""".toRegex()
        )

        for (pattern in patterns) {
            val match = pattern.find(command)
            if (match != null) {
                return match.groupValues[1].toDoubleOrNull()
            }
        }
        return null
    }

    /**
     * Extract count parameter
     */
    private fun extractCount(command: String): Int? {
        val patterns = listOf(
            """(?:scrollDown|scrollUp)\s*\(\s*(\d+)\s*\)""".toRegex(),
            """(\d+)""".toRegex()
        )

        for (pattern in patterns) {
            val match = pattern.find(command)
            if (match != null) {
                return match.groupValues[1].toIntOrNull()
            }
        }
        return null
    }

    /**
     * Extract selector and timeout from waitForSelector command
     */
    private fun extractSelectorAndTimeout(command: String): Pair<String?, Long?> {
        val selectorPattern = """waitForSelector\s*\(\s*['"]([^'"]+)['"]""".toRegex()
        val timeoutPattern = """(\d+)L?""".toRegex()

        val selector = selectorPattern.find(command)?.groupValues?.get(1)
        val timeout = timeoutPattern.findAll(command).lastOrNull()?.groupValues?.get(1)?.toLongOrNull()

        return Pair(selector, timeout)
    }

    /**
     * Extract URL from navigateTo command
     */
    private fun extractUrl(command: String): String? {
        val patterns = listOf(
            """navigateTo\s*\(\s*['"]([^'"]+)['"]""".toRegex(),
            """['"]((https?://[^'"]+))['"]""".toRegex()
        )

        for (pattern in patterns) {
            val match = pattern.find(command)
            if (match != null) {
                return match.groupValues[1]
            }
        }
        return null
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

        // Try to find the method in webDriverSourceCode
        val methodPattern = """fun\s+$methodName\s*\([^)]*\)[^{]*""".toRegex()
        val match = methodPattern.find(webDriverSourceCode)

        if (match != null) {
            // Look for documentation comment before the method
            val methodIndex = webDriverSourceCode.indexOf(match.value)
            val beforeMethod = webDriverSourceCode.substring(0, methodIndex)

            // Find the last /** */ comment block before this method
            val docPattern = """/\*\*([^*]|\*(?!/))*\*/""".toRegex()
            val docMatches = docPattern.findAll(beforeMethod).toList()

            if (docMatches.isNotEmpty()) {
                val lastDoc = docMatches.last().value
                // Extract description from the comment
                return extractDescriptionFromComment(lastDoc)
            }
        }

        // Fallback to pattern-based description generation
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
    suspend fun generateWebDriverActionsOptimized(prompt: String, driver: WebDriver?): ActionDescription {
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
        val WEB_DRIVER_MESSAGE_TEMPLATE = """
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

        val PULSAR_SESSION_MESSAGE_TEMPLATE = """
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
    }
}
