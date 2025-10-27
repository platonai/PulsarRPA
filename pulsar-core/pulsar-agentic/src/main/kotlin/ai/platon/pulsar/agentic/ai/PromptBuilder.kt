package ai.platon.pulsar.agentic.ai

import ai.platon.pulsar.agentic.ai.agent.ExtractParams
import ai.platon.pulsar.agentic.ai.agent.ObserveParams
import ai.platon.pulsar.agentic.ai.support.AgentTool
import ai.platon.pulsar.browser.driver.chrome.dom.DOMSerializer
import ai.platon.pulsar.browser.driver.chrome.dom.model.DOMState
import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.serialize.json.Pson
import ai.platon.pulsar.skeleton.ai.AgentState
import java.time.LocalDate
import java.util.*
import kotlin.math.min

/**
 * Description:
 * Builder for language-localized prompt snippets used by agentic browser tasks.
 *
 * Prompt key points:
 * - Locale-aware (CN/EN) output
 * - Produces structured fragments for system/user roles
 * - Minimizes extra text to steer LLM behavior
 */
class PromptBuilder() {

    companion object {
        var locale: Locale = Locale.CHINESE

        val isZH = locale in listOf(Locale.CHINESE, Locale.SIMPLIFIED_CHINESE, Locale.TRADITIONAL_CHINESE)

        val ACTION_SCHEMA = """
        |{"domain": string, "method": string, "arguments": [{"name": string, "value": string}],
        |"currentPageContentSummary": string,
        |"actualLastActionImpact": string, "expectedNextActionImpact": string
        |}
        |""".trimMargin()

        fun buildObserveResultSchema(returnAction: Boolean): String {
            val actionFields = if (returnAction) ACTION_SCHEMA else ""

            val schema = """
{
  "elements": [
    {
      "locator": string,
      "description": string$actionFields
    }
  ]
}
""".let { Strings.compactWhitespaces(it) }

            return schema
        }

        fun buildObserveResultSchemaContract(params: ObserveParams): String {
            val schema = buildObserveResultSchema(params.returnAction)

            return if (isZH) {
                """
## Schema 要求
你必须返回一个与以下模式匹配的有效 JSON 对象：
$schema

""".trimIndent()
            } else {
                """
## Schema Contract
You MUST respond with a valid JSON object matching this schema:
$schema

""".trimIndent()
            }
        }

        val AGENT_GUIDE_SYSTEM_PROMPT = """
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
${buildObserveResultSchema(true)}

2) 任务完成输出：

{"taskComplete":bool,"summary":string,"keyFindings":[string],"nextSuggestions":[string]}

## 安全要求：
- 仅操作可见的交互元素
- 遇到验证码或安全提示时停止执行

## 工具规范：

```kotlin
${AgentTool.TOOL_CALL_SPECIFICATION}
```

- domain: 方法的调用方，如 driver, browser 等
- 将 `locator` 视为 `selector`
- 确保 `locator` 与对应的无障碍树节点属性完全匹配，准确定位该节点
- 不提供不能确定的参数
- 要求 json 输出时，禁止包含任何额外文本
- 注意：用户难以区分按钮和链接
- 若操作与页面无关，返回空数组
- 只返回一个最相关的操作
- 如需连续点击打开多个链接，使用 click(selector, "Ctrl") 在新标签页打开
- 按键操作（如"按回车"），用press方法（参数为"A"/"Enter"/"Space"）。特殊键首字母大写。。不要模拟点击屏幕键盘上的按键
- 仅对特殊按键（如 Enter、Tab、Escape）进行首字母大写
- 如果需要操作前一页面，但已跳转，使用 `goBack`

## 无障碍树（Accessibility Tree）说明：

无障碍树包含页面 DOM 关键节点的主要信息，包括节点文本内容，可见性，可交互性，坐标和尺寸等。

- 节点唯一定位符 `locator` 由两个整数组成。
- 所有节点可见，除非 `invisible` == true 显式指定。
- 除非显式指定，`scrollable` 为 false, `interactive` 为 false。
- 对于坐标和尺寸，若未显式赋值，则视为 `0`。涉及属性：`clientRects`, `scrollRects`, `bounds`。

请基于当前页面截图、无障碍树与历史动作，规划下一步（严格单步原子动作）。

        """.trimIndent()

        fun compactPrompt(prompt: String, maxWidth: Int = 200): String {
            val brief = prompt
                .replace(AGENT_GUIDE_SYSTEM_PROMPT, "{{AGENT_GUIDE_SYSTEM_PROMPT}}")
                .replace(AgentTool.TOOL_CALL_SPECIFICATION, "{{TOOL_CALL_SPECIFICATION}}")
            return Strings.compactWhitespaces(brief, maxWidth)
        }
    }

    fun buildOperatorSystemPrompt(): String {
        return """
$AGENT_GUIDE_SYSTEM_PROMPT
        """.trimIndent()
    }

    private fun buildSystemPromptV20251025(
        url: String,
        executionInstruction: String,
        systemInstructions: String? = null
    ): String {
        return if (systemInstructions != null) {
            """
        $systemInstructions
        Your current goal: $executionInstruction
        """.trimIndent()
        } else {
            """
        You are a web automation assistant using browser automation tools to accomplish the user's goal.

        Your task: $executionInstruction

        You have access to various browser automation tools. Use them step by step to complete the task.

        IMPORTANT GUIDELINES:
        1. Always start by understanding the current page state
        2. Use the screenshot tool to verify page state when needed
        3. Use appropriate tools for each action
        4. When the task is complete, use the "close" tool with success: true
        5. If the task cannot be completed, use "close" with success: false

        TOOLS OVERVIEW:
        - screenshot: Take a compressed JPEG screenshot for quick visual context (use sparingly)
        - ariaTree: Get an accessibility (ARIA) hybrid tree for full page context (preferred for understanding layout and elements)
        - act: Perform a specific atomic action (click, type, etc.). For filling a field, you can say 'fill the field x with the value y'.
        - extract: Extract structured data
        - goto: Navigate to a URL
        - wait/navback/refresh: Control timing and navigation
        - scroll: Scroll the page x pixels up or down

        STRATEGY:
        - Prefer ariaTree to understand the page before acting; use screenshot for quick confirmation.
        - Keep actions atomic and verify outcomes before proceeding.

        For each action, provide clear reasoning about why you're taking that step.
        Today's date is ${LocalDate.now()}. You're currently on the website: ${url}.
        """.trimIndent()
        }
    }

    fun buildObserveGuideSystemExtraPrompt(userProvidedInstructions: String?): SimpleMessage? {
        if (userProvidedInstructions.isNullOrBlank()) return null

        val contentCN = """
***用户自定义指令***

在执行操作时请牢记用户的指令。如果这些指令与当前任务无关，请忽略。

用户指令：
$userProvidedInstructions
""".trim()

        val contentEN = """
***Custom Instructions Provided by the User***

Please keep the user's instructions in mind when performing actions. If the user's instructions are not relevant to the current task, ignore them.

User Instructions:
$userProvidedInstructions
""".trim()

        val content = if (locale == Locale.CHINESE) contentCN else contentEN

        return SimpleMessage("system", content)
    }

    fun buildExtractSystemPrompt(userProvidedInstructions: String? = null): SimpleMessage {
        val baseInstructionCN = """你正在代表用户提取内容。
如果用户要求你提取“列表”信息或“全部”信息，
你必须提取用户请求的所有信息。

你将获得：
1. 一条指令
2. 一个要从中提取内容的 DOM 元素列表"""

        val baseInstructionEN = """You are extracting content on behalf of a user.
If a user asks you to extract a 'list' of information, or 'all' information,
YOU MUST EXTRACT ALL OF THE INFORMATION THAT THE USER REQUESTS.

You will be given:
1. An instruction
2. A list of DOM elements to extract from"""

        val instructionsCN = """
从 DOM 元素中原样打印精确文本，包含所有符号、字符和换行。
如果没有发现新的信息，打印 null 或空字符串。
  """.trim()
        val instructionsEN = """
Print the exact text from the DOM elements with all symbols, characters, and endlines as is.
Print null or an empty string if no new information is found.
  """.trim()

        val additionalInstructionsCN =
            "如果用户尝试提取链接或 URL，则你必须仅返回链接元素的 ID。不要尝试直接从文本中提取链接，除非绝对必要。 "
        val additionalInstructionsEN =
            "If a user is attempting to extract links or URLs, you MUST respond with ONLY the IDs of the link elements. " +
                    "Do not attempt to extract links directly from the text unless absolutely necessary. "

        val userInstructions = buildObserveGuideSystemExtraPrompt(userProvidedInstructions)

        val baseInstruction = if (isZH) baseInstructionCN else baseInstructionEN
        val instructions = if (isZH) instructionsCN else instructionsEN
        val additionalInstructions = if (isZH) additionalInstructionsCN else additionalInstructionsEN

        val content = "$baseInstruction\n$instructions\n$additionalInstructions\n$userInstructions"

        return SimpleMessage(role = "system", content = content)
    }

    fun buildAgentStateHistoryMessage(history: List<AgentState>): String {
        if (history.isEmpty()) return ""

        val his = history.takeLast(min(8, history.size))
            .joinToString("\n") { Pson.toJson(it) }

        val msg = """
此前动作摘要：
$his
		""".trimIndent()

        return msg
    }

    fun buildOverallGoalMessage(overallGoal: String): String {
        val msg = """
总体目标：
<overallGoal>
$overallGoal
</overallGoal>
                """.trimIndent()

        return msg
    }

    fun initExtractUserInstruction(instruction: String? = null): String {
        if (instruction.isNullOrBlank()) {
            return if (isZH) {
                "从网页中提取关键数据结构"
            } else {
                "Extract key structured data from the page"
            }
        }

        return instruction
    }

    fun buildExtractDomContent(domState: DOMState, params: ExtractParams): String {
        val json = domState.nanoTreeLazyJson

        // Inject schema hint to strongly guide JSON output
        val hintCN = "你必须返回一个严格符合以下JSON Schema的有效JSON对象。不要包含任何额外说明。"
        val hintEN =
            "You MUST respond with a valid JSON object that strictly conforms to the following JSON Schema. Do not include any extra commentary."
        val hint = if (isZH) hintCN else hintEN

        return buildString {
            append(json)
            append("\n\n$hint")
            append("\nJSON Schema:\n")
            append(params.schema)
        }
    }

    fun buildExtractUserPrompt(instruction: String, domContent: String): SimpleMessage {
        return buildExtractUserPrompt(instruction, domContent, null)
    }

    fun buildExtractUserPrompt(instruction: String, domContent: String, browserStateJson: String?): SimpleMessage {
        val instructionLabel = if (isZH) "指令: " else "Instruction: "
        val browserStateLabel = if (isZH) "当前浏览器状态" else "Current Browser State"
        val domLabel = if (isZH) "DOM: " else "DOM: "

        val sb = StringBuilder()
            .append(instructionLabel)
            .append('\n')
            .append(instruction)
            .append('\n')

        if (browserStateJson != null) {
            sb.append('\n')
                .append("## ")
                .append(browserStateLabel)
                .append('\n')
                .append(browserStateJson)
                .append('\n')
        }

        sb.append(domLabel)
            .append('\n')
            .append(domContent)

        return SimpleMessage(role = "user", content = sb.toString())
    }

    private val metadataSystemPromptCN: String = """
你是一名 AI 助手，负责评估一次抽取任务的进展和完成状态。
请分析抽取响应，判断任务是否已经完成或是否需要更多信息。
严格遵循以下标准：
1. 一旦当前抽取响应已经满足了指令，必须将完成状态设为 true 并停止处理，不论是否还有剩余分片。
2. 只有在以下两个条件同时成立时，才将完成状态设为 false：
   - 指令尚未被满足
   - 仍然有剩余分片需要处理（chunksTotal > chunksSeen）
每个 chunk 表示一屏网页内容，第一屏对应第一个 chunk。
""".trimIndent()

    private val metadataSystemPromptEN: String = """
You are an AI assistant tasked with evaluating the progress and completion status of an extraction task.
Analyze the extraction response and determine if the task is completed or if more information is needed.
Strictly abide by the following criteria:
1. Once the instruction has been satisfied by the current extraction response, ALWAYS set completion status to true and stop processing, regardless of remaining chunks.
2. Only set completion status to false if BOTH of these conditions are true:
   - The instruction has not been satisfied yet
   - There are still chunks left to process (chunksTotal > chunksSeen)
Each chunk corresponds to one viewport-sized section of the page (the first chunk is the first screen).
""".trimIndent()

    fun buildMetadataSystemPrompt(): SimpleMessage {
        val content = if (isZH) metadataSystemPromptCN else metadataSystemPromptEN
        return SimpleMessage(
            role = "system",
            content = content,
        )
    }

    fun buildMetadataPrompt(
        instruction: String,
        extractionResponse: Any,
        chunksSeen: Int,
        chunksTotal: Int,
    ): SimpleMessage {
        val extractedJson = DOMSerializer.MAPPER.writeValueAsString(extractionResponse)

        val content = if (isZH) {
            """
指令: $instruction
提取结果: $extractedJson
已处理分片数: $chunksSeen
总分片数: $chunksTotal
""".trim()
        } else {
            """
Instruction: $instruction
Extracted content: $extractedJson
chunksSeen: $chunksSeen
chunksTotal: $chunksTotal
""".trim()
        }

        return SimpleMessage(role = "user", content = content)
    }

    fun buildObservePrompt(params: ObserveParams): AgentMessageList {
        // observe guide
        val messages = AgentMessageList()
        val systemMsg = buildObserveGuideSystemPrompt(params.userProvidedInstructions)
        messages.add(systemMsg)
        // instruction + DOM + browser state + schema
        buildObserveUserMessage(messages, params)

        return messages
    }

    fun buildObserveGuideSystemPrompt(userProvidedInstructions: String? = null): SimpleMessage {
        fun observeSystemPromptCN() = """
你正在通过根据用户希望观察的页面内容来查找元素，帮助用户实现浏览器操作自动化。
你将获得：
- 一条关于待观察元素的指令
- 一个展示页面语义结构的分层无障碍树（accessibility tree）。该树是DOM（文档对象模型）与无障碍树的混合体。

如果存在符合指令的元素，则返回这些元素的数组；否则返回空数组。

## 无障碍树（Accessibility Tree）说明：

无障碍树包含页面 DOM 关键节点的主要信息，包括节点文本内容，可见性，可交互性，坐标和尺寸等。

- 节点唯一定位符 `locator` 由两个整数组成。
- 所有节点可见，除非 `invisible` == true 显式指定。
- 除非显式指定，`scrollable` 为 false, `interactive` 为 false。
- 对于坐标和尺寸，若未显式赋值，则视为 `0`。涉及属性：`clientRects`, `scrollRects`, `bounds`。
"""

        fun observeSystemPromptEN() = """
You are helping the user automate the browser by finding elements based on what the user wants to observe in the page.

You will be given:
- an instruction of elements to observe
- a hierarchical accessibility tree showing the semantic structure of the page. The tree is a hybrid of the DOM and the accessibility tree.

Return an array of elements that match the instruction if they exist, otherwise return an empty array.

## 无障碍树（Accessibility Tree）说明：

无障碍树包含页面 DOM 关键节点的主要信息，包括节点文本内容，可见性，可交互性，坐标和尺寸等。

- 节点唯一定位符 `locator` 由两个整数组成。
- 所有节点可见，除非 `invisible` == true 显式指定。
- 除非显式指定，`scrollable` 为 false, `interactive` 为 false。
- 对于坐标和尺寸，若未显式赋值，则视为 `0`。涉及属性：`clientRects`, `scrollRects`, `bounds`。
"""

        val observeSystemPrompt = if (isZH) observeSystemPromptCN() else observeSystemPromptEN()
        val extra = buildObserveGuideSystemExtraPrompt(userProvidedInstructions)?.content
        val content = if (extra != null) "$observeSystemPrompt\n\n$extra" else observeSystemPrompt

        return SimpleMessage(role = "system", content = content)
    }

    fun initObserveUserInstruction(instruction: String?): String {
        return when {
            !instruction.isNullOrBlank() -> instruction
            isZH -> """
查找页面中可用于后续任何操作的元素，包括导航链接、相关页面链接、章节/子章节链接、按钮或其他交互元素。
请尽可能全面：如果存在多个可能与未来操作相关的元素，需全部返回。
                """.trimIndent()

            else -> """
Find elements that can be used for any future actions in the page. These may be navigation links,
related pages, section/subsection links, buttons, or other interactive elements.
Be comprehensive: if there are multiple elements that may be relevant for future actions, return all of them.
                """.trimIndent()
        }
    }

    fun buildObserveUserMessage(messages: AgentMessageList, params: ObserveParams) {
        val instruction = params.instruction
        val browserStateJson = params.browserUseState.browserState.lazyJson
        val nanoTreeJson = params.browserUseState.domState.nanoTreeLazyJson

        if (isZH) {
            messages.addUser("指令: $instruction")
        } else {
            messages.addUser("instruction: $instruction")
        }

        val schemaContract = buildObserveResultSchemaContract(params)
        fun contentCN() = """
## 无障碍树(Accessibility Tree):
$nanoTreeJson

## 当前浏览器状态
$browserStateJson

$schemaContract
"""

        fun contentEN() = """
## Accessibility Tree:
$nanoTreeJson

## Current Browser State
$browserStateJson

$schemaContract
"""

        val content = when {
            isZH -> contentCN()
            else -> contentEN()
        }

        messages.add("user", content)
    }

    fun buildObserveActToolUsePrompt(
        action: String, toolCalls: List<String>, variables: Map<String, String>? = null
    ): String {
        // Base instruction
        val instruction = if (isZH) {
            """
根据以下动作查找最相关的页面元素：$action。为该元素提供一个工具来执行该动作。分析执行后的影响和预期结果。

## 支持的工具列表

```kotlin
${toolCalls.joinToString("\n")}
```

- domain: 方法的调用方，如 `driver`, `browser` 等
- 将 `locator` 视为 `selector`
- 确保 `locator` 与对应的无障碍树节点属性完全匹配，准确定位该节点
- 不提供不能确定的参数
- 要求 json 输出时，禁止包含任何额外文本
- 注意：用户难以区分按钮和链接
- 若操作与页面无关，返回空数组
- 只返回一个最相关的操作
- 如需连续点击打开多个链接，使用 click(selector, "Ctrl") 在新标签页打开
- 按键操作（如"按回车"），用press方法（参数为"A"/"Enter"/"Space"）。特殊键首字母大写。不要模拟点击屏幕键盘上的按键
- 仅对特殊按键（如 Enter、Tab、Escape）进行首字母大写
- 如果需要操作前一页面，但已跳转，使用 `goBack`
""".trimIndent()
        } else {
            """
Find the most relevant element to perform an action on given the following action: $action.
Provide a tool to perform the action for this element. Analyze the impact and expected outcomes after execution.

## Supported Tool List

```kotlin
${toolCalls.joinToString("\n")}
```

• Treat locator as selector.
• Ensure locator exactly matches the corresponding accessibility tree node attributes to accurately locate the node.
• Do not provide uncertain parameters.
• When JSON output is required, prohibit any additional text.
• Note: Users cannot easily distinguish buttons from links.
• If the action is unrelated to the page, return an empty array.
• Return only the most relevant single action.
• To access multiple links for research, use click(selector, "Ctrl") to open in a new tab.
• For key actions (e.g., "press Enter"), use the press method (parameters: "A"/"Enter"/"Space"). Capitalize special keys. Do not simulate clicks on an on-screen keyboard.
• Capitalize only special keys (e.g., Enter, Tab, Escape).
• To interact with the previous page after navigation, use goBack.
""".trimIndent()
        }

        return instruction
    }
}
