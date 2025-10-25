package ai.platon.pulsar.agentic.ai

import ai.platon.pulsar.agentic.ai.agent.ExtractParams
import ai.platon.pulsar.agentic.ai.agent.ObserveParams
import ai.platon.pulsar.browser.driver.chrome.dom.DOMSerializer
import ai.platon.pulsar.browser.driver.chrome.dom.model.DOMState
import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.alwaysFalse
import java.time.LocalDate
import java.util.*
import kotlin.math.min

data class SimpleMessage(
    val role: String,
    val content: String,
    val name: String? = null,
) {
    override fun toString() = content
}

class SimpleMessageList(
    val messages: MutableList<SimpleMessage> = mutableListOf()
) {
    fun addSystem(content: String, key: String? = null) {
        add("system", content, key)
    }

    fun addUser(content: String, key: String? = null) {
        add("user", content, key)
    }

    fun add(role: String, content: String, key: String? = null) {
        val msg = SimpleMessage(role, content, key)
        messages.add(msg)
    }

    fun add(message: SimpleMessage) {
        val msg = SimpleMessage(message.role, message.content, message.name)
        messages.add(msg)
    }

    fun find(key: String): SimpleMessage? {
        return messages.find { it.name == key }
    }

    fun systemMessages() = messages.filter { it.role == "system" }

    fun userMessages() = messages.filter { it.role == "user" }
}

/**
 * Description:
 * Builder for language-localized prompt snippets used by agentic browser tasks.
 *
 * Prompt key points:
 * - Locale-aware (CN/EN) output
 * - Produces structured fragments for system/user roles
 * - Minimizes extra text to steer LLM behavior
 */
class PromptBuilder(val locale: Locale = Locale.CHINESE) {

    val isCN = locale in listOf(Locale.CHINESE, Locale.SIMPLIFIED_CHINESE, Locale.TRADITIONAL_CHINESE)


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










    /**
     * Description:
     * Builds the custom-instructions snippet (localized). Returns empty when input is blank.
     *
     * Prompt key points:
     * - Echo user-provided instructions verbatim
     * - Remind to follow only when relevant
     * - Localized header/title (CN/EN)
     * - Returns "" if instructions are null/blank
     */
    fun buildUserInstructionsString(userProvidedInstructions: String?): String {
        if (userProvidedInstructions.isNullOrBlank()) return ""

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

        return if (locale == Locale.CHINESE) contentCN else contentEN
    }

    // extract
    /**
     * Description:
     * Builds the extraction system prompt emphasizing full compliance, exact-text printing, and link-ID only policy.
     *
     * Prompt key points:
     * - Extract ALL information when user asks for list/all
     * - Print exact DOM text with symbols/newlines preserved
     * - For links/URLs, return element IDs only
     * - May append user custom instructions
     * - Returns a system-role message
     */
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

        val userInstructions = buildUserInstructionsString(userProvidedInstructions)

        val baseInstruction = if (isCN) baseInstructionCN else baseInstructionEN
        val instructions = if (isCN) instructionsCN else instructionsEN
        val additionalInstructions = if (isCN) additionalInstructionsCN else additionalInstructionsEN

        val content = "$baseInstruction\n$instructions\n$additionalInstructions\n$userInstructions"

        return SimpleMessage(role = "system", content = content)
    }

    /**
     * Description:
     * Provides a default extraction user instruction when none is specified.
     *
     * Prompt key points:
     * - Defaults to extracting key structured data from the page
     * - Localized default text (CN/EN)
     * - Returns the provided instruction if non-blank
     */
    fun initExtractUserInstruction(instruction: String? = null): String {
        if (instruction.isNullOrBlank()) {
            return if (isCN) {
                "从网页中提取关键数据结构"
            } else {
                "Extract key structured data from the page"
            }
        }

        return instruction
    }

    /**
     * Description:
     * Builds the DOM content fragment for extraction with nanoTree JSON and a strict JSON Schema hint.
     *
     * Prompt key points:
     * - Includes page nanoTree JSON
     * - Appends a strict JSON Schema the model MUST follow
     * - Explicitly forbids any extra commentary
     * - Strongly steers JSON-only output
     */
    fun buildExtractDomContent(domState: DOMState, params: ExtractParams): String {
        val json = domState.nanoTreeLazyJson

        // Inject schema hint to strongly guide JSON output
        val hintCN = "你必须返回一个严格符合以下JSON Schema的有效JSON对象。不要包含任何额外说明。"
        val hintEN =
            "You MUST respond with a valid JSON object that strictly conforms to the following JSON Schema. Do not include any extra commentary."
        val hint = if (isCN) hintCN else hintEN

        return buildString {
            append(json)
            append("\n\n$hint")
            append("\nJSON Schema:\n")
            append(params.schema)
        }
    }

    /**
     * Description:
     * Builds the extraction user message composed of the instruction and DOM content.
     *
     * Prompt key points:
     * - Two sections: Instruction and DOM
     * - Uses localized labels
     * - Returns a user-role message
     */
    fun buildExtractUserPrompt(instruction: String, domContent: String): SimpleMessage {
        return buildExtractUserPrompt(instruction, domContent, null)
    }

    /**
     * Description:
     * Builds the extraction user message with optional browser state.
     *
     * Prompt key points:
     * - Multiple sections: Instruction, Browser State (optional), and DOM
     * - Uses localized labels
     * - Returns a user-role message
     */
    fun buildExtractUserPrompt(instruction: String, domContent: String, browserStateJson: String?): SimpleMessage {
        val instructionLabel = if (isCN) "指令: " else "Instruction: "
        val browserStateLabel = if (isCN) "当前浏览器状态" else "Current Browser State"
        val domLabel = if (isCN) "DOM: " else "DOM: "

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

    /**
     * Description:
     * Builds the system prompt for metadata evaluation of extraction progress/completion.
     *
     * Prompt key points:
     * - Stop immediately once instruction is satisfied
     * - Continue only if not satisfied AND chunks remain
     * - Defines chunk as one viewport screen
     * - Returns a system-role message
     */
    fun buildMetadataSystemPrompt(): SimpleMessage {
        val content = if (isCN) metadataSystemPromptCN else metadataSystemPromptEN
        return SimpleMessage(
            role = "system",
            content = content,
        )
    }

    /**
     * Description:
     * Builds the user message for metadata evaluation with instruction, extracted JSON, and chunk counters.
     *
     * Prompt key points:
     * - Includes instruction and serialized extraction result
     * - Provides chunksSeen and chunksTotal for progress context
     * - Aids completion decision logic
     * - Returns a user-role message
     */
    fun buildMetadataPrompt(
        instruction: String,
        extractionResponse: Any,
        chunksSeen: Int,
        chunksTotal: Int,
    ): SimpleMessage {
        val extractedJson = DOMSerializer.MAPPER.writeValueAsString(extractionResponse)

        val content = if (isCN) {
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

    // observe
    /**
     * Description:
     * Builds the observe system prompt: instruction + hybrid accessibility tree; return matching elements or empty.
     *
     * Brief:
     * observe guide
     *
     * Prompt key points:
     * - Input: observation instruction + accessibility/DOM hybrid tree
     * - Output: array of matching elements, or [] if none
     * - Can append user custom instructions
     * - Returns a system-role message
     */
    fun buildObserveGuideSystemPrompt(userProvidedInstructions: String? = null): SimpleMessage {
        fun observeSystemPromptCN() = """
你正在通过根据用户希望观察的页面内容来查找元素，帮助用户实现浏览器操作自动化。
你将获得：
- 一条关于待观察元素的指令
- 一个展示页面语义结构的分层无障碍树（accessibility tree）。该树是DOM（文档对象模型）与无障碍树的混合体。

如果存在符合指令的元素，则返回这些元素的数组；否则返回空数组。
"""

        fun observeSystemPromptEN() = """
You are helping the user automate the browser by finding elements based on what the user wants to observe in the page.

You will be given:
- an instruction of elements to observe
- a hierarchical accessibility tree showing the semantic structure of the page. The tree is a hybrid of the DOM and the accessibility tree.

Return an array of elements that match the instruction if they exist, otherwise return an empty array.
"""

        val observeSystemPrompt = if (isCN) observeSystemPromptCN() else observeSystemPromptEN()
        val extra = buildUserInstructionsString(userProvidedInstructions)
        val content = if (extra.isNotBlank()) "$observeSystemPrompt\n\n$extra" else observeSystemPrompt

        return SimpleMessage(role = "system", content = content)
    }

    /**
     * Description:
     * Provides a default observe user instruction when none is specified.
     *
     * Prompt key points:
     * - Default asks for interactive/navigational elements for future actions
     * - Encourage comprehensive coverage (links, buttons, sections, related pages)
     * - Localized default text (CN/EN)
     */
    fun initObserveUserInstruction(instruction: String?): String {
        return when {
            !instruction.isNullOrBlank() -> instruction
            isCN -> """
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

    /**
     * Description:
     * Builds the observe user message containing the instruction, accessibility tree/spec, browser state, and schema.
     *
     * Brief:
     * instruction + DOM + browser state + schema
     *
     * Prompt key points:
     * - Includes the hierarchical accessibility tree (nanoTree JSON)
     * - States visibility/scrollable/interactive defaults and locator format
     * - Includes current browser state JSON
     * - Appends the observe-result schema contract
     * - Returns a user-role message
     */
    fun buildObserveUserMessage(messages: SimpleMessageList, params: ObserveParams) {
        val instruction = params.instruction
        val browserStateJson = params.browserUseState.browserState.lazyJson
        val nanoTreeJson = params.browserUseState.domState.nanoTreeLazyJson

        if (isCN) {
            messages.addUser("指令: $instruction")
        } else {
            messages.addUser("instruction: $instruction")
        }

        val schemaContract = buildObserveResultSchemaContract(params)
        fun contentCN() = """
## 无障碍树(Accessibility Tree):
$nanoTreeJson

## 无障碍树说明：
- 节点唯一定位符 `locator` 由两个整数组成。
- 所有节点可见，除非 `invisible` == true 显示指定。
- 除非显式指定，`scrollable` 为 false, `interactive` 为 false。
- 对于坐标和尺寸，若未显式赋值，则视为 `0`。涉及属性：`clientRects`, `scrollRects`, `bounds`。

## 当前浏览器状态
$browserStateJson

$schemaContract
"""

        fun contentEN() = """
## Accessibility Tree:
$nanoTreeJson

## Accessibility Tree Specification
- The unique node identifier `locator` consists of two integers.
- All nodes are visible unless `invisible` == true explicitly.
- Unless explicitly specified, `scrollable` is false, `interactive` is false.
- For values in `clientRects`, `scrollRects`, `bounds`, treated as `0` if no explicit value assigned.

## Current Browser State
$browserStateJson

$schemaContract
"""

        val content = when {
            isCN -> contentCN()
            else -> contentEN()
        }

        messages.add("user", content)
    }

    /**
     * Description:
     * Builds the schema contract for observe results returned by the model.
     *
     * Prompt key points:
     * - JSON object with { elements: [ { locator, description, (optional) method, arguments } ] }
     * - If returnAction=true, include method and arguments
     * - Locator must exactly match the accessibility tree node
     * - Selector is derived from locator; no undetermined params
     * - No extra text beyond valid JSON
     */
    fun buildObserveResultSchemaContract(params: ObserveParams): String {
        // Build schema hint for the LLM (prompt-enforced)

        val actionFields = if (params.returnAction) {
            """, "method": string, "arguments": [{"name": string, "value": string}] """
        } else ""

        // reserved
        val overallGoalState = if (alwaysFalse() && params.overallGoal != null) {
            """, "overallGoalState": { "isComplete": boolean, "summary": string, "suggestions": [string] }"""
        } else ""

        val schema = """
{
  "elements": [
    {
      "locator": string,
      "description": string$actionFields
    }
  ]$overallGoalState
}
""".let { Strings.compactWhitespaces(it) }

        return if (isCN) {
            """
## Schema 要求
你必须返回一个与以下模式匹配的有效 JSON 对象：
$schema

- 确保 `locator` 与对应的无障碍树节点属性完全匹配，可以定位该节点
- 工具调用时，`selector` 参数将基于 `locator`
- 不提供不能确定的参数
- 禁止包含任何额外文本
""".trimIndent()
        } else {
            """
## Schema Contract
You MUST respond with a valid JSON object matching this schema:
$schema

- The `locator` **must exactly match** the corresponding accessibility tree node attributes to ensure correct node identification.
- During tool invocation, the `selector` parameter **is derived from** the `locator`.
- Do not provide parameters that cannot be determined
- Must not include any extra text
""".trimIndent()
        }
    }

    /**
     * Description:
     * Builds a tool-use instruction mapping a user action to the most relevant element and available tool.
     *
     * Prompt key points:
     * - Lists supported tool calls for selection
     * - Reminds that buttons and links are perceptually similar
     * - If action is unrelated, return an empty array
     * - Return only one most relevant action
     * - Localized content (CN/EN)
     */
    @Suppress("UNUSED_PARAMETER")
    fun buildToolUsePrompt(
        action: String, toolCalls: List<String>, variables: Map<String, String>? = null
    ): String {
        // Base instruction
        val instruction = if (isCN) {
            """
根据以下动作查找最相关的页面元素：$action。为该元素提供一个工具来执行该动作。

## 支持的工具列表

```kotlin
${toolCalls.joinToString("\n")}
```

请注意：对用户而言，按钮和链接在大多数情况下看起来是一样的。
如果该动作与页面上可能采取的操作完全无关，返回空数组。
只返回一个动作。如果有多个相关动作，返回最相关的一个。
""".trimIndent()
        } else {
            """
Find the most relevant element to perform an action on given the following action: $action.
Provide a tool to perform the action for this element.

## Supported Tool List

```kotlin
${toolCalls.joinToString("\n")}
```

Remember that to users, buttons and links look the same in most cases.
If the action is completely unrelated to a potential action to be taken on the page, return an empty array.
ONLY return one action. If multiple actions are relevant, return the most relevant one.
""".trimIndent()
        }

        return instruction
    }
}
