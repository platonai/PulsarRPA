package ai.platon.pulsar.agentic.ai.tta

import ai.platon.pulsar.browser.driver.chrome.dom.model.BrowserUseState
import ai.platon.pulsar.browser.driver.chrome.dom.model.DOMTreeNodeEx
import ai.platon.pulsar.browser.driver.chrome.dom.model.SnapshotOptions
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.ExperimentalApi
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
import com.google.gson.JsonObject
import com.google.gson.JsonParser
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
    open suspend fun generate(instruction: String, driver: WebDriver, screenshotB64: String? = null): ActionDescription {
        require(driver is PulsarWebDriver) { "PulsarWebDriver is required to use agents" }
        val browserUseState = driver.domService.getBrowserUseState(SnapshotOptions())

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
    open suspend fun generate(instruction: String, browserUseState: BrowserUseState, screenshotB64: String? = null): ActionDescription {
        try {
            return generateWithToolCallSpecs(instruction, browserUseState, screenshotB64, 1)
        } catch (e: Exception) {
            val errorResponse = ModelResponse(
                """
                suspend fun llmGeneratedFunction(driver: WebDriver) {
                    // Error occurred during optimization: ${e.message}
                }
            """.trimIndent(), ResponseState.OTHER
            )
            return ActionDescription(emptyList(), errorResponse, null, null)
        }
    }

    @ExperimentalApi
    open suspend fun generateResponse(
        instruction: String, browserUseState: BrowserUseState? = null, screenshotB64: String? = null, toolCallLimit: Int = 100,
    ): ModelResponse {
        val systemPrompt = when {
            instruction.contains(AGENT_SYSTEM_PROMPT_PREFIX_20) -> instruction
            else -> buildOperatorSystemPrompt(instruction)
        }

        val stateMessage = buildBrowserUseStatePrompt(browserUseState, toolCallLimit)
        val response = if (screenshotB64 != null) {
            chatModel.call(systemPrompt, stateMessage, null, screenshotB64, "image/jpeg")
        } else {
            chatModel.call(systemPrompt, stateMessage)
        }

        return response
    }

    @ExperimentalApi
    private suspend fun generateWithToolCallSpecs(
        instruction: String, browserUseState: BrowserUseState? = null, screenshotB64: String? = null, toolCallLimit: Int = 100,
    ): ActionDescription {
        val response = generateResponse(instruction, browserUseState, screenshotB64, toolCallLimit)

        return modelResponseToActionDescription(response, toolCallLimit)
    }

    fun buildBrowserUseStatePrompt(browserUseState: BrowserUseState? = null, toolCallLimit: Int = 100): String {
        if (browserUseState == null) {
            return "每次最多调用 $toolCallLimit 个工具。"
        }

        val prompt = """
每次最多调用 $toolCallLimit 个工具。

## 可交互元素列表
${browserUseState.domState.nanoTreeLazyJson}

- 节点唯一定位符 `locator` 由两个整数组成。
- 所有节点可见，除非 `invisible` == true 显示指定。
- 除非显式指定，`scrollable` 为 false, `interactive` 为 false。
- 对于坐标和尺寸，若未显式赋值，则视为 `0`。涉及属性：`clientRects`, `scrollRects`, `bounds`。

"""

        return prompt
    }

    fun buildOperatorSystemPrompt(goal: String): String {
        return """
$TTA_AGENT_SYSTEM_PROMPT

用户总目标：$goal
        """.trimIndent()
    }

    /**
     * Response format:
     *
     * ```json
     * {
     *   tool_calls: [ { name: string, args: [name: string, value: string] } ]
     *   taskComplete: boolean
     * }
     * ```
     * */
    protected fun modelResponseToActionDescription(response: ModelResponse, toolCallLimit: Int = 1): ActionDescription {
        val toolCalls = parseToolCalls(response.content).take(toolCallLimit)
        val functionCalls = if (toolCalls.isNotEmpty()) {
            toolCalls.mapNotNull { toolCallToDriverLine(it) }
        } else {
            response.content.split("\n").map { it.trim() }.filter { it.startsWith("driver.") && it.contains("(") }
        }
        return ActionDescription(functionCalls, response, null, null)
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

                val args = mutableMapOf<String, Any?>()
                val argsEl = obj.get("args")
                if (argsEl?.isJsonObject == true) {
                    val argsObj: JsonObject = argsEl.asJsonObject
                    for ((k, v) in argsObj.entrySet()) {
                        args[k] = jsonElementToKotlin(v)
                    }
                } else if (argsEl?.isJsonArray == true) {
                    val argsArr = argsEl.asJsonArray
                    // Heuristic: if it's a list of strings, create numbered keys
                    argsArr.forEachIndexed { index, jsonElement ->
                        args["arg${index + 1}"] = jsonElementToKotlin(jsonElement)
                    }
                }

                ToolCall("driver", name, args)
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

    internal fun toolCallToDriverLine(tc: ToolCall): String? = ToolCallExecutor.toolCallToExpression(tc)

    suspend fun getInteractiveElements(driver: WebDriver): List<InteractiveElement> {
        require(driver is PulsarWebDriver)
        val snapshotOptions = SnapshotOptions()

        val trees = driver.domService.getMultiDOMTrees(options = snapshotOptions)
        val root = driver.domService.buildEnhancedDomTree(trees)

        fun buildSelector(node: DOMTreeNodeEx): String {
            val tag = node.nodeName.lowercase()
            val attrs = node.attributes
            val id = attrs["id"]?.takeIf { it.isNotBlank() }
            if (!id.isNullOrBlank()) return "#${'$'}{escapeIdent(id)}"

            val testId = attrs["data-testid"]?.takeIf { it.isNotBlank() }
            if (!testId.isNullOrBlank()) return "[data-testid=\"${'$'}{escapeAttrValue(testId)}\"]"

            val nameAttr = attrs["name"]?.takeIf { it.isNotBlank() }
            if (!nameAttr.isNullOrBlank()) return "${'$'}tag[name=\"${'$'}{escapeAttrValue(nameAttr)}\"]"

            val onclick = attrs["onclick"]?.takeIf { it.isNotBlank() }
            if (!onclick.isNullOrBlank()) return "${'$'}tag[onclick=\"${'$'}{escapeAttrValue(onclick)}\"]"

            val href = attrs["href"]?.takeIf { it.isNotBlank() }
            if (tag == "a" && !href.isNullOrBlank()) return "a[href=\"${'$'}{escapeAttrValue(href)}\"]"

            val typeAttr = attrs["type"]?.takeIf { it.isNotBlank() }
            if (!typeAttr.isNullOrBlank()) return "${'$'}tag[type=\"${'$'}{escapeAttrValue(typeAttr)}\"]"

            val classes = attrs["class"]?.trim()?.split(Regex("\\s+"))?.filter { it.isNotBlank() } ?: emptyList()
            if (classes.isNotEmpty()) {
                val top = classes.take(2).joinToString("") { ".${'$'}{escapeIdent(it)}" }
                return "${'$'}tag${'$'}top"
            }

            return tag
        }

        fun isInteractiveCandidate(node: DOMTreeNodeEx): Boolean {
            val tag = node.nodeName.uppercase()
            val attrs = node.attributes
            val hasHref = !attrs["href"].isNullOrBlank()
            val role = node.axNode?.role?.lowercase()
            val standardInteractive = tag in setOf("INPUT", "TEXTAREA", "SELECT", "BUTTON") || (tag == "A" && hasHref)
            val roleInteractive = role in setOf("button", "link", "checkbox", "textbox", "combobox", "menuitem")
            return node.isInteractable == true || standardInteractive || roleInteractive
        }

        fun boundsOf(node: DOMTreeNodeEx): ElementBounds? {
            val r = node.snapshotNode?.clientRects ?: node.snapshotNode?.bounds ?: node.absolutePosition
            return r?.let { ElementBounds(it.x, it.y, it.width, it.height) }
        }

        fun visible(node: DOMTreeNodeEx, b: ElementBounds?): Boolean {
            val v = node.isVisible
            val hasSize = b != null && b.width > 0 && b.height > 0
            return when (v) {
                true -> hasSize
                false -> false
                null -> hasSize
            }
        }

        val results = mutableListOf<Pair<Int, InteractiveElement>>()

        fun traverse(node: DOMTreeNodeEx) {
            if (isInteractiveCandidate(node)) {
                val b = boundsOf(node)
                if (visible(node, b)) {
                    val attrs = node.attributes
                    val selector = buildSelector(node)
                    val tag = node.nodeName.uppercase()
                    val text = listOfNotNull(
                        node.axNode?.name?.takeIf { !it.isNullOrBlank() },
                        attrs["aria-label"],
                        attrs["title"],
                        attrs["value"]
                    ).firstOrNull()?.take(100) ?: ""

                    val element = InteractiveElement(
                        id = attrs["id"] ?: "",
                        tagName = tag,
                        selector = selector,
                        text = text,
                        type = attrs["type"],
                        href = attrs["href"],
                        className = attrs["class"],
                        placeholder = attrs["placeholder"],
                        value = attrs["value"],
                        isVisible = true,
                        bounds = b ?: ElementBounds(0.0, 0.0, 0.0, 0.0)
                    )
                    val order = node.interactiveIndex ?: Int.MAX_VALUE
                    results += order to element
                }
            }
            node.children.forEach { traverse(it) }
            node.shadowRoots.forEach { traverse(it) }
            node.contentDocument?.let { traverse(it) }
        }

        traverse(root)

        return results
            .sortedBy { it.first }
            .map { it.second }
            .distinctBy { it.selector }
    }

    companion object {

        val TTA_AGENT_SYSTEM_PROMPT = """
你是一个网页通用代理，目标是基于用户目标一步一步完成任务。
重要指南：
1) 将复杂动作拆成原子步骤；
2) 一次仅做一个动作（如：单击一次、输入一次、选择一次）；
3) 不要在一步中合并多个动作；
4) 多个动作用多步表达；
5) 始终验证目标元素存在且可见后再执行操作；
6) 遇到错误时尝试替代方案或优雅终止；

## 输出严格使用 JSON 字段：

{
  tool_calls: [ { name: string, args: [name: string, value: string] } ]
  taskComplete: boolean
}

## 安全要求：
- 仅操作可见的交互元素
- 避免快速连续操作，适当等待页面加载
- 遇到验证码或安全提示时停止执行

## 工具规范：
```
${ToolCallExecutor.TOOL_CALL_LIST}
```

        """.trimIndent()

        val AGENT_SYSTEM_PROMPT_PREFIX_20 = TTA_AGENT_SYSTEM_PROMPT.take(20)
    }
}
