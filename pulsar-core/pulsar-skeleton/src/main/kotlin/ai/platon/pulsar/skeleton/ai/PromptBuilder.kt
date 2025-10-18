package ai.platon.pulsar.skeleton.ai

import ai.platon.pulsar.skeleton.ai.agent.ExtractParams
import ai.platon.pulsar.skeleton.ai.agent.ObserveParams
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.codehaus.jackson.map.ObjectMapper
import java.util.*

class PromptBuilder(val locale: Locale = Locale.CHINESE) {

    data class ChatMessage(
        val role: String,
        val content: String
    )

    val isCN = locale in listOf(Locale.CHINESE, Locale.SIMPLIFIED_CHINESE, Locale.TRADITIONAL_CHINESE)

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
    fun buildExtractSystemPrompt(
        userProvidedInstructions: String? = null,
    ): ChatMessage {
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

        return ChatMessage(role = "system", content = content)
    }

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

    fun buildExtractDomContent(domText: String, params: ExtractParams): String {
        // Inject schema hint to strongly guide JSON output
        val hintCN = "你必须返回一个严格符合以下JSON Schema的有效JSON对象。不要包含任何额外说明。"
        val hintEN =
            "You MUST respond with a valid JSON object that strictly conforms to the following JSON Schema. Do not include any extra commentary."
        val hint = if (isCN) hintCN else hintEN

        return buildString {
            append(domText)
            append("\n\n$hint")
            append("\nJSON Schema:\n")
            append(params.schema)
        }
    }

    fun buildExtractUserPrompt(instruction: String, domContent: String): ChatMessage {
        val instructionLabel = if (isCN) "指令: " else "Instruction: "
        val domLabel = if (isCN) "DOM: " else "DOM: "

        val sb = StringBuilder()
            .append(instructionLabel)
            .append('\n')
            .append(instruction)
            .append('\n')
            .append(domLabel)
            .append('\n')
            .append(domContent)

        return ChatMessage(role = "user", content = sb.toString())
    }

    private val metadataSystemPromptCN: String = """
你是一名 AI 助手，负责评估一次抽取任务的进展和完成状态。
请分析抽取响应，判断任务是否已经完成或是否需要更多信息。
严格遵循以下标准：
1. 一旦当前抽取响应已经满足了指令，必须将完成状态设为 true 并停止处理，不论是否还有剩余分片。
2. 只有在以下两个条件同时成立时，才将完成状态设为 false：
   - 指令尚未被满足
   - 仍然有剩余分片需要处理（chunksTotal > chunksSeen）
""".trimIndent()

    private val metadataSystemPromptEN: String = """
You are an AI assistant tasked with evaluating the progress and completion status of an extraction task.
Analyze the extraction response and determine if the task is completed or if more information is needed.
Strictly abide by the following criteria:
1. Once the instruction has been satisfied by the current extraction response, ALWAYS set completion status to true and stop processing, regardless of remaining chunks.
2. Only set completion status to false if BOTH of these conditions are true:
   - The instruction has not been satisfied yet
   - There are still chunks left to process (chunksTotal > chunksSeen)
""".trimIndent()

    fun buildMetadataSystemPrompt(): ChatMessage {
        val content = if (isCN) metadataSystemPromptCN else metadataSystemPromptEN
        return ChatMessage(
            role = "system",
            content = content,
        )
    }

    fun buildMetadataPrompt(
        instruction: String,
        extractionResponse: Any,
        chunksSeen: Int,
        chunksTotal: Int,
    ): ChatMessage {
        val mapper = jacksonObjectMapper()
        val extractedJson = runCatching {
            mapper.writerWithDefaultPrettyPrinter().writeValueAsString(extractionResponse)
        }.getOrElse { _ ->
            // Fallback to toString if serialization fails
            extractionResponse.toString()
        }

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

        return ChatMessage(role = "user", content = content)
    }

    // observe
    fun buildObserveSchemaHint(params: ObserveParams, schemaHint: Boolean = false): String {
        // Build dynamic schema hint for the LLM (prompt-enforced)
        if (!schemaHint) {
            return ""
        }

        val hint = buildString {
            if (isCN) {
                append("""你必须返回一个与以下模式匹配的有效 JSON 对象: { "elements": [ { "elementId": string, "description": string""")
                if (params.returnAction) {
                    append(""", "method": string, "arguments": [string]""")
                }
                append(" } ] } 。elementId 必须遵循 'number-number' 格式，且不得包含方括号。不要包含任何额外文本。")
            } else {
                append("""You MUST respond with a valid JSON object matching this schema: { "elements": [ { "elementId": string, "description": string""")
                if (params.returnAction) {
                    append(""", "method": string, "arguments": [string]""")
                }
                append(" } ] } . The elementId must follow the 'number-number' format and MUST NOT include square brackets. Do not include any extra text.")
            }
        }

        return hint
    }

    // observe
    fun buildObserveSystemPrompt(
        userProvidedInstructions: String? = null
    ): ChatMessage {
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

        return ChatMessage(role = "system", content = content)
    }

    fun initObserveUserInstruction(instruction: String?): String {
        return when {
            !instruction.isNullOrBlank() -> instruction
            isCN -> """
                Find elements that can be used for any future actions in the page. These may be navigation links,
                related pages, section/subsection links, buttons, or other interactive elements.
                Be comprehensive: if there are multiple elements that may be relevant for future actions, return all of them.
                """.trimIndent()

            else -> """
                Find elements that can be used for any future actions in the page. These may be navigation links,
                related pages, section/subsection links, buttons, or other interactive elements.
                Be comprehensive: if there are multiple elements that may be relevant for future actions, return all of them.
                """.trimIndent()
        }
    }

    fun buildObserveUserMessage(instruction: String, params: ObserveParams): ChatMessage {
        val browserStateJson = ObjectMapper().writeValueAsString(params.browserState.basicState)

        val schemaHint = buildObserveSchemaHint(params, schemaHint = true)
        fun contentCN() = """
指令: $instruction

无障碍树(Accessibility Tree):
${params.browserState.domState.json}

当前浏览器状态：
$browserStateJson

$schemaHint
"""

        fun contentEN() = """
instruction: $instruction

Accessibility Tree:
${params.browserState.domState.json}

Current Browser State:
$browserStateJson

$schemaHint
"""

        val content = when {
            isCN -> contentCN()
            else -> contentEN()
        }

        return ChatMessage(role = "user", content = content)
    }

    /**
     * Builds the instruction for the observeAct method to find the most relevant element for an action
     */
    fun buildActObservePrompt(
        action: String,
        toolCalls: List<String>,
        variables: Map<String, String>? = null,
    ): String {
        // Base instruction
        val instruction = if (isCN) {
            """
根据以下动作查找最相关的页面元素：$action。
为该元素提供一个动作，支持的动作如下（Kotlin 语法）：

```
${toolCalls.joinToString("\n")}
```

请注意：对用户而言，按钮和链接在大多数情况下看起来是一样的。
如果该动作与页面上可能采取的操作完全无关，返回空数组。
只返回一个动作。如果有多个相关动作，返回最相关的一个。
""".trimIndent()
        } else {
            """
Find the most relevant element to perform an action on given the following action: $action.
Provide an action for this element, supported actions (Kotlin syntax):

```
${toolCalls.joinToString("\n")}
```

Remember that to users, buttons and links look the same in most cases.
If the action is completely unrelated to a potential action to be taken on the page, return an empty array.
ONLY return one action. If multiple actions are relevant, return the most relevant one.
""".trimIndent()
        }

        return instruction
    }

    fun buildOperatorSystemPrompt(goal: String): ChatMessage {
        val contentCN = """
你是一名通用型代理，负责通过在页面上执行操作，在多次模型调用中完成用户的目标。

你将获得一个目标以及到目前为止所采取步骤的列表。你的任务是判断用户的目标是否已经完成，或者是否仍需要继续执行后续步骤。

# 你当前的目标
$goal

# 重要：你必须使用提供的工具来执行操作。不要只描述你打算做什么——要实际调用合适的工具。

# 可用工具及其使用时机：
- `act`：用于与页面交互（点击、输入、导航等）
- `extract`：用于从页面获取信息
- `goto`：用于导航到指定 URL
- `wait`：用于等待一段时间
- `navback`：用于返回上一页
- `refresh`：用于刷新当前页面
- `close`：仅在任务完成或无法实现时使用
- 外部工具：根据你的目标需要，可使用其他附加工具（例如搜索工具）

# 重要指南
1. 始终使用工具——不要只提供关于计划的文本响应
2. 将复杂操作拆解为独立的原子步骤
3. 对于 `act` 命令，每次只执行一个动作，例如：
   - 对某个具体元素执行单次点击
   - 在一个输入框中输入内容
   - 选择一个选项
4. 避免在一个指令中组合多个动作
5. 如果需要多个动作，应将它们作为独立步骤依次执行
6. 只有当任务真正完成或无法实现时才使用 `close`
""".trimIndent()

        val contentEN = """
You are a general-purpose agent whose job is to accomplish the user's goal across multiple model calls by running actions on the page.

You will be given a goal and a list of steps that have been taken so far. Your job is to determine if either the user's
goal has been completed or if there are still steps that need to be taken.

# Your current goal
$goal

# CRITICAL: You MUST use the provided tools to take actions. Do not just describe what you want to do - actually call the appropriate tools.

# Available tools and when to use them:
- `act`: Use this to interact with the page (click, type, navigate, etc.)
- `extract`: Use this to get information from the page
- `goto`: Use this to navigate to a specific URL
- `wait`: Use this to wait for a period of time
- `navback`: Use this to go back to the previous page
- `refresh`: Use this to refresh the current page
- `close`: Use this ONLY when the task is complete or cannot be achieved
- External tools: Use any additional tools (like search tools) as needed for your goal

# Important guidelines
1. ALWAYS use tools - never just provide text responses about what you plan to do
2. Break down complex actions into individual atomic steps
3. For `act` commands, use only one action at a time, such as:
   - Single click on a specific element
   - Type into a single input field
   - Select a single option
4. Avoid combining multiple actions in one instruction
5. If multiple actions are needed, they should be separate steps
6. Only use `close` when the task is genuinely complete or impossible to achieve
""".trimIndent()

        val content = if (isCN) contentCN else contentEN

        return ChatMessage(
            role = "system",
            content = content,
        )
    }
}
