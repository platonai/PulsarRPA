package ai.platon.pulsar.agentic.ai

import ai.platon.pulsar.agentic.ai.agent.ExtractParams
import ai.platon.pulsar.agentic.ai.agent.ObserveParams
import ai.platon.pulsar.agentic.ai.support.AgentTool
import ai.platon.pulsar.browser.driver.chrome.dom.DOMSerializer
import ai.platon.pulsar.browser.driver.chrome.dom.model.DOMState
import ai.platon.pulsar.browser.driver.chrome.dom.model.TabState
import ai.platon.pulsar.common.KStrings
import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.skeleton.ai.AgentState
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.time.LocalDate
import java.util.*
import kotlin.math.min
import kotlin.math.roundToInt

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

        val language = if (isZH) "中文" else "English"

        const val MAX_ACTIONS = 1

        val ACTION_SCHEMA = """
        |,
        |"domain": string, "method": string, "arguments": [{"name": string, "value": string}],
        |"screenshotContentSummary": string, "currentPageContentSummary": string,
        |"memory": string,"thinking": string,
        |"evaluationPreviousGoal": string, "nextGoal": string,
        |
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

---

""".trimIndent()
            } else {
                """
## Schema Contract
You MUST respond with a valid JSON object matching this schema:
$schema

---

""".trimIndent()
            }
        }

        val TOOL_CALL_RULE_CONTENT = """
严格遵循以下规则使用浏览器和浏览网页：

- domain: 方法的调用方，如 driver, browser 等
- 输出结果中，定位节点时 `selector` 字段始终填入 `locator` 的值
- 确保 `locator` 与对应的无障碍树节点属性完全匹配，准确定位该节点
- 不提供不能确定的参数
- 要求 json 输出时，禁止包含任何额外文本
- 注意：用户难以区分按钮和链接
- 若操作与页面无关，返回空对象 `{}`
- 只返回一个最相关的操作
- 从`## 浏览器状态`段落获得所有打开标签页的信息
- 如需检索信息，新建标签页而非复用当前页
- 使用 `click(selector, "Ctrl")` 新建标签页，在**新标签页**打开链接
- 如果目标页面在**新标签页**打开，使用 `browser.switchTab(tabId: String)` 切换到目标页面，从`## 浏览器状态`段落获得 `tabId`
- 若页面因输入文本等操作发生变化，需判断是否要交互新出现的元素（例如从列表中选择正确选项）。
- 如需阅读整个网页文本，如总结信息，使用 `textContent`
- 按键操作（如"按回车"），用press方法（参数为"A"/"Enter"/"Space"）。特殊键首字母大写。不要模拟点击屏幕键盘上的按键
- 仅对特殊按键（如 Enter、Tab、Escape）进行首字母大写
- 如果需要操作前一页面，但已跳转，使用 `goBack`
- 如非必要，避免重复点击同一链接，如必须这样做，提供理由
- 若出现验证码，尽可能尝试解决；若无法解决，则启用备用策略（例如换其他站点、回退上一步）
- 若预期元素缺失，尝试刷新页面、滚动或返回上一页
- 若填写输入框后操作序列中断，通常是因为页面发生了变化（例如输入框下方弹出了建议选项）
- 若上一步操作序列因页面变化而中断，需补全未执行的剩余操作。例如，若你尝试输入文本并点击搜索按钮，但点击未执行（因页面变化），应在下一步重试点击操作。
- 若<user_request>中包含具体页面信息（如商品类型、评分、价格、地点等），尝试使用筛选功能以提高效率。
- 若向字段输入内容，无需先滚动和聚焦，工具内部处理
- 若向字段输入内容，可能需要按回车、点击搜索按钮或从下拉菜单选择以完成操作。
- 如无必要，不要登录页面。没有凭证时，绝对不要尝试登录。
- 始终考虑最终目标：<user_request>包含的内容。若用户指定了明确步骤，这些步骤始终具有最高优先级。
- 始终先判断任务属于两类哪一种：
  1. 非常具体的逐步指令
    - 精确地遵循这些步骤，不要跳过，尽力完成每一项要求。
  2. 开放式任务：
    - 自行规划并有创造性地完成任务。
    - 如果你在开放式任务中被卡住（例如遇到登录或验证码），可以重新评估任务并尝试替代方案，例如有时即使出现登录弹窗，页面的某些部分仍可访问，或者可以通过网络搜索获得信息。

    """.trimIndent()

        val INTERACTIVE_ELEMENT_LIST_NOTE_CONTENT = """

可交互元素列表(interactive elements)包含页面 DOM 可交互元素的主要信息，包括元素简化 HTML 表示，文本内容，前后文本，所在视口，坐标和大小等。

列表格式：
[locator]{viewport}(x,y,width,height)<slimNode>textContent</slimNode>Text-Before-This-Interactive-Element-And-After-Previous-Interactive-Element

- 默认列出当前焦点视口，第1，2视口和最后一视口元素。
- `locator` 为节点唯一定位符，同无障碍树保持一致，由两个整数构成，不含括号。
- 输出结果中，定位节点时 `selector` 字段始终填入 `locator` 的值。
- `viewport` 为节点所在视口序号，1-based，不含括号。
- 注意：网页内容变化可能导致视口位置随时发生变化。
- `x,y,width,height` 为节点坐标和尺寸。

        """.trimIndent()

        val A11Y_TREE_NOTE_CONTENT = """

无障碍树包含页面 DOM 关键节点的主要信息，包括节点文本内容，可见性，可交互性，坐标和尺寸等。

- 除非特别指定，无障碍树仅包含网页当前视口内的节点信息，并包含少量视口外节点，以保证信息充分。
- 节点唯一定位符 `locator` 由两个整数组成。
- 所有节点可见，除非 `invisible` == true 显式指定。
- 除非显式指定，`scrollable` 为 false, `interactive` 为 false。
- 对于坐标和尺寸，若未显式赋值，则视为 `0`。涉及属性：`clientRects`, `scrollRects`, `bounds`。

        """.trimIndent()

        val AGENT_GUIDE_SYSTEM_PROMPT = """
你是一个被设计为在迭代循环中运行以自动化浏览器任务的 AI 代理。你的最终目标是完成 <user_request> 中提供的任务。

# 系统指南

## 总体要求

你擅长以下任务：
1. 浏览复杂网站并提取精确信息
2. 自动化表单提交与交互式网页操作
3. 收集并保存信息
4. 有效使用文件系统来决定在上下文中保留哪些内容
5. 在代理循环中高效运行
6. 高效地执行各类网页任务

---

## 语言设置

- 默认工作语言：**$language**
- 始终以与用户请求相同的语言回复

---

## 输入

在每一步，你的输入将包括：
1. `## 智能体历史`：按时间顺序的事件流，包含你之前的动作及其结果。
2. `## 智能体状态`：当前的 <user_request>、<file_system> 摘要、<todo_contents> 和 <step_info> 摘要。
3. `## 浏览器状态`：当前 URL、打开的标签页、可交互元素的索引及可见页面内容。
4. `## 视觉信息`：浏览器截图。如果你之前使用过截图，这里将包含截图。

## 智能体历史

智能体历史包含一系列步骤信息。

单步信息示例：
```json
{"step":1,"action":"action","description":"description","screenshotContentSummary":"screenshotContentSummary","currentPageContentSummary":"currentPageContentSummary","evaluationPreviousGoal":"evaluationPreviousGoal","nextGoal":"nextGoal","url":"https://example.com/","timestamp":1762076188.31}
```

---

## 用户请求

用户请求（USER REQUEST）：这是你的最终目标并始终可见。
- 它具有最高优先级。使用户满意。
- 如果用户请求非常具体——则要仔细遵循每一步，不要跳过或凭空编造步骤。
- 如果任务是开放式的，你可以自行规划完成方式。

---

## 浏览器状态

浏览器状态包括：
- 当前 URL：你当前查看页面的 URL。
- 打开的标签页：带有 id 的打开标签页。

---

## 视觉信息

- 如果你之前使用过截图，你将获得当前页面的截图。
- 视觉信息是首要事实依据（GROUND TRUTH）：在推理中利用图像来评估你的进展。
- 在推理中利用图像来评估你的进展。
- 当不确定或想获取更多信息时使用截图。

---

## 工具列表

```kotlin
${AgentTool.TOOL_CALL_SPECIFICATION}
```

$TOOL_CALL_RULE_CONTENT

---

## 可交互元素

$INTERACTIVE_ELEMENT_LIST_NOTE_CONTENT

---

## 无障碍树

$A11Y_TREE_NOTE_CONTENT

---

## 数据提取

- 上一步操作的数据提取结果，如 `textContent`

---

## 任务完成规则

你必须在以下三种情况之一结束任务，按照`任务完成输出`格式要求输出相应 json 格式：
- 当你已完全完成 USER REQUEST。
- 当达到允许的最大步骤数（`max_steps`）时，即使任务未完成也要完成。
- 如果绝对无法继续，也要完成。

`任务完成输出` 是你终止并与用户共享发现结果的机会。
- 仅当完整地、无缺失地完成 USER REQUEST 时，将 `success` 设为 `true`。
- 如果有任何部分缺失、不完整或不确定，将 `success` 设为 `false`。
- 如果用户要求特定格式（例如：“返回具有以下结构的 JSON”或“以指定格式返回列表”），确保在回答中使用正确的格式。
- 如果用户要求结构化输出，`任务完成输出` 的 schema 将被修改。解决任务时请考虑该 schema。

---

## 动作规则

- 在每一步中你允许使用最多 {$MAX_ACTIONS} 个动作。
  - 如果允许多个动作，明确多个动作按顺序执行（一个接一个）。
- 如果页面在动作后发生了改变，序列会被中断并返回新的状态。

---

## 效率指南

- 如需输入，直接输入，无需滚动和聚焦，工具层处理
- 屏幕阅读规则
  - 逐屏阅读，屏幕视觉内容是推理的最终依据
  - 例外
    - 如果你的任务仅依赖网页文本，不要逐屏阅读，滚动到网页底部 + 使用文本提取工具(selectFirstTextOrNull)直接获取网页内容
- 不要在一步中尝试多条不同路径。始终为每一步设定一个明确目标。重要的是在下一步你能看到动作是否成功，因此不要链式调用会多次改变浏览器状态的动作，例如：
   - 不要使用 click 然后再 navigate，因为你无法确认 click 是否成功。
   - 不要连续使用 switch，因为你看不到中间状态。
   - 不要使用 input 然后立即 scroll，因为你无法验证 input 是否生效。

---

## 推理规则

在每一步的 `thinking` 块中，你必须明确且系统化地进行推理。
为成功完成 <user_request> 请遵循以下推理模式：

- 基于 <agent_history> 推理，以追踪朝向 <user_request> 的进展与上下文。
- 分析 <agent_history> 中最近的 `nextGoal` 与 `evaluationPreviousGoal`，并明确说明你之前尝试达成的目标。
- 分析所有相关的 <agent_history>、<browser_state> 和截图以了解当前状态。
- 明确判断上一步动作的成功/失败/不确定性。不要仅仅因为上一步在 <agent_history> 中显示已执行就认为成功。例如，你可能记录了 “动作 1/1：在元素 3 中输入 '2025-05-05'”，但输入实际上可能失败。始终使用 <browser_vision>（截图）作为主要事实依据；如果截图不可用，则备选使用 <browser_state>。若预期变化缺失，请将上一步标记为失败（或不确定），并制定恢复计划。
- 分析你是否陷入了重复无进展的状态；若是，考虑替代方法，例如滚动以获取更多上下文、使用发送键（`press`）直接模拟按键，或换用不同页面。
- 决定应存储在记忆中的简明、可操作的上下文以供后续推理使用。
- 在准备结束时，说明你准备输出`taskComplete`并向用户报告完成情况/结果。
- 始终关注 <user_request>。仔细分析所需的具体步骤和信息，例如特定筛选条件、表单字段等，确保当前轨迹与用户请求一致。

---

## 示例

下面是一些良好输出模式的示例。可参考但不要直接复制。


### 评估示例

- 正面示例：
"evaluationPreviousGoal": "已成功导航到商品页面并找到了目标信息。结论：成功"
"evaluationPreviousGoal": "已点击登录按钮并显示了用户认证表单。结论：成功"
- 负面示例：
"evaluationPreviousGoal": "无法在图像中看到搜索栏，因此未能在搜索栏输入文本。结论：失败"
"evaluationPreviousGoal": "点击索引为 15 的提交按钮但表单未成功提交。结论：失败"

---

### 记忆示例

"memory": "已访问 5 个目标网站中的 2 个。从 Amazon（$39.99）和 eBay（$42.00）收集了价格数据。仍需检查 Walmart、Target 和 Best Buy。"
"memory": "在主页面发现许多待处理报告。已成功处理前两个季度销售数据报告，接下来处理库存分析和客户反馈。"

---

### 下一目标示例

"nextGoal": "点击 '加入购物车' 按钮以继续购买流程。"
"nextGoal": "提取页面第一个项目的详细信息。"

---

## 输出

输出严格使用以下两种 JSON 之一:

1) 动作输出（最多一个元素）：
${buildObserveResultSchema(true)}

2) 任务完成输出：

{"taskComplete":bool,"success":bool,"summary":string,"keyFindings":[string],"nextSuggestions":[string]}

## 安全要求：
- 仅操作可见的交互元素
- 遇到验证码或安全提示时停止执行

---

        """.trimIndent()

        fun compactPrompt(prompt: String, maxWidth: Int = 200): String {
            val boundaries = """
你正在通过根据用户希望观察的页面内容来查找元素
否则返回空数组。

## 支持的工具列表
---

## 无障碍树
---
            """.trimIndent()

            val boundaryPairs = boundaries.split("\n").filter { it.isNotBlank() }.chunked(2).map { it[0] to it[1] }

            val compacted = KStrings.replaceContentInSections(prompt, boundaryPairs, "\n...\n\n")

            return Strings.compactLog(compacted, maxWidth)
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
        if (history.isEmpty()) {
            return ""
        }

        val mapper: ObjectMapper = jacksonObjectMapper().apply {
            setSerializationInclusion(JsonInclude.Include.NON_NULL)
            setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
            registerModule(JavaTimeModule())
        }

        val his = history.takeLast(min(8, history.size))
            .joinToString("\n") { mapper.writeValueAsString(it) }

        val msg = """
## 智能体历史

<agent_history>
$his
</agent_history>

---

		""".trimIndent()

        return msg
    }

    fun buildAgentStateMessage(state: AgentState): String {
        val message = """
## 智能体状态

当前的 <user_request>、<file_system> 摘要、<todo_contents> 和 <step_info> 摘要。

---

        """.trimIndent()

        return message
    }

    fun buildBrowserVisionInfo(): String {
        val visionInfo = """
## 视觉信息

- 在推理中利用图像来评估你的进展。
- 当不确定或想获取更多信息时使用截图。

[Current page screenshot provided as base64 image]

---

""".trimIndent()

        return visionInfo
    }

    fun buildExtractTextContentMessage(agentState: AgentState, domContent: String): String {
        return """
## 数据提取

上一步操作：${agentState.prevState?.action}
上一步操作期望结果：${agentState.prevState?.nextGoal}

上一步操作数据提取结果：

<dom_content>
$domContent
</dom_content>

---

        """.trimIndent()
    }

    fun buildUserRequestMessage(userRequest: String): String {
        val msg = """
# 当前任务

## 用户输入

<user_request>
$userRequest
</user_request>

请基于当前页面截图、可交互元素列表、无障碍树与智能体历史，规划下一步（严格单步原子动作）。

---

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
        val json = domState.microTree.toNanoTreeInRange().lazyJson

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
        val instructionLabel = if (isZH) "指令: \n" else "Instruction: \n"
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
        nextChunkToSee: Int,
        chunksTotal: Int,
    ): SimpleMessage {
        val extractedJson = DOMSerializer.MAPPER.writeValueAsString(extractionResponse)

        val content = if (isZH) {
            """
## 元数据

指令: $instruction
提取结果: $extractedJson
待处理分片: $nextChunkToSee
总分片数: $chunksTotal

每个分片对应一个视口高度，第 i 个待处理分片指第 i 视口内所有 DOM nodes。

---

""".trim()
        } else {
            """
## Metadata

Instruction: $instruction
Extracted content: $extractedJson
nextChunkToSee: $nextChunkToSee
chunksTotal: $chunksTotal

Each chunk represents one viewport-height range.
The i-th chunk to process contains all DOM nodes located within the i-th viewport.

---

""".trim()
        }

        return SimpleMessage(role = "user", content = content)
    }

    fun buildObservePrompt(messages: AgentMessageList, params: ObserveParams) {
        // observe guide
        val systemMsg = buildObserveGuideSystemPrompt(params.userProvidedInstructions)
        messages.addFirst(systemMsg)
        // DOM + browser state + schema
        buildObserveUserMessage(messages, params)
    }

    fun buildObserveGuideSystemPrompt(userProvidedInstructions: String? = null): SimpleMessage {
        fun observeSystemPromptCN() = """
你正在通过根据用户希望观察的页面内容来查找元素，帮助用户实现浏览器操作自动化。
你将获得：
- 一条关于待观察元素的指令
- 一个包含网页所有可交互元素信息的列表
- 一个展示页面语义结构的分层无障碍树（accessibility tree）。该树是DOM（文档对象模型）与无障碍树的混合体。

如果存在符合指令的元素，则返回这些元素的数组；否则返回空数组。

## 可交互元素列表（Interactive Elements）说明：

$INTERACTIVE_ELEMENT_LIST_NOTE_CONTENT

---

## 无障碍树（Accessibility Tree）说明：

$A11Y_TREE_NOTE_CONTENT

---

"""

        fun observeSystemPromptEN() = """
你正在通过根据用户希望观察的页面内容来查找元素，帮助用户实现浏览器操作自动化。
你将获得：
- 一条关于待观察元素的指令
- 一个包含网页所有可交互元素信息的列表
- 一个展示页面语义结构的分层无障碍树（accessibility tree）。该树是DOM（文档对象模型）与无障碍树的混合体。

如果存在符合指令的元素，则返回这些元素的数组；否则返回空数组。

## 可交互元素列表（Interactive Elements）说明：

$INTERACTIVE_ELEMENT_LIST_NOTE_CONTENT

---

## 无障碍树（Accessibility Tree）说明：

$A11Y_TREE_NOTE_CONTENT

---

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
        val prevBrowserState = params.agentState.prevState?.browserUseState?.browserState
        val browserState = params.browserUseState.browserState

        val prevTabs = prevBrowserState?.tabs ?: emptyList()
        val currentTabs = browserState.tabs
        val newTabs: List<TabState> = if (prevTabs.size != currentTabs.size) {
            currentTabs - prevTabs.toSet()
        } else emptyList()
        val newTabsJson = if (newTabs.isNotEmpty()) DOMSerializer.toJson(newTabs) else null
        val newTabsMessage = if (newTabs.isEmpty()) "" else {
            """
上一步新打开的标签页：

$newTabsJson

            """.trimIndent()
        }

        val scrollState = browserState.scrollState
        // Height in pixels of the page area above the current viewport. (被隐藏在视口上方的部分的高度)
        val hiddenTopHeight = scrollState.y.roundToInt().coerceAtLeast(0)
        val hiddenBottomHeight = (scrollState.totalHeight - hiddenTopHeight - scrollState.viewport.height)
            .roundToInt().coerceAtLeast(0)
        val viewportHeight = scrollState.viewport.height
        val domState = params.browserUseState.domState

        // The 1-based viewport to see.
        val processingViewport = (hiddenTopHeight / viewportHeight) + 1
        val viewportsTotal = params.browserUseState.browserState.scrollState.viewportsTotal

        val interactiveElements = domState.microTree.toInteractiveDOMTreeNodeList(
            currentViewportIndex = processingViewport, maxViewportIndex = viewportsTotal)

        val startY = hiddenTopHeight - viewportHeight * 0.5
        val endY = hiddenTopHeight + viewportHeight + viewportHeight * 0.5
        val nanoTree = domState.microTree.toNanoTreeInRange(startY, endY)
        // val nanoTree = domState.microTree.toNanoTree()

        val schemaContract = buildObserveResultSchemaContract(params)

        fun contentCN() = """
## 浏览器状态

<browser_state>
${browserState.lazyJson}
</browser_state>

$newTabsMessage

---

## 视口信息

本次焦点视口序号: $processingViewport
视口高度：$viewportHeight
估算视口总数: $viewportsTotal
视口之上像素高度: $hiddenTopHeight
视口之下像素高度: $hiddenBottomHeight

- 默认每次查看一个视口高度(viewport height)内的所有 DOM 节点
- 视口之上像素高度: 当前视口上方、已滚动出可视范围的网页内容高度，单位为像素（px）。
- 视口之下像素高度: 当前视口下方、已滚动出可视范围的网页内容高度，单位为像素（px）。
- 注意：网页内容变化可能导致视口位置和视口序号随时发生变化。
- 默认提供的无障碍树仅包含第`i`个视口内的 DOM 节点，并包含少量视口外节点，以保证信息完整
- 如需查看下一视口，调用 `scrollBy(viewportHeight)` 向下滚动一屏获取更多信息

## 可交互元素

格式概要：

[locator]{viewport}(x,y,width,height)<slimNode>textContent</slimNode>Text-Before-This-Interactive-Element-And-After-Previous-Interactive-Element

聚焦第${processingViewport}视口可交互元素。

${interactiveElements.lazyString}

## 无障碍树(Accessibility Tree)

聚焦第${processingViewport}视口节点。

```json
${nanoTree.lazyJson}
```

---

$schemaContract

"""

        // TODO: we need a translation
        fun contentEN() = contentCN()

        val content = when {
            isZH -> contentCN()
            else -> contentEN()
        }

        messages.addLast("user", content)
    }

    fun buildObserveActToolUsePrompt(action: String): String {
        val instruction =
            """
## 用户指令
根据以下动作查找最相关的页面元素：$action。为该元素提供一个工具来执行该动作。分析执行后的影响和预期结果。

---

"""

        return instruction
    }

    fun buildObserveActToolSpecsPrompt(
        messages: AgentMessageList, toolCalls: List<String>, variables: Map<String, String>? = null
    ) {
        // Base instruction
        val toolSpecs = if (isZH) {
            """
## 支持的工具列表

```kotlin
${toolCalls.joinToString("\n")}
```

$TOOL_CALL_RULE_CONTENT

---

""".trimIndent()
        } else {
            """
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

---

""".trimIndent()
        }

        messages.addSystem(toolSpecs, "toolSpecs")
    }

    fun buildSummaryPrompt(goal: String, stateHistory: List<AgentState>): Pair<String, String> {
        val system = "你是总结助理，请基于执行轨迹对原始目标进行总结，输出 JSON。"
        val user = buildString {
            appendLine("原始目标：$goal")
            appendLine("执行轨迹(按序)：")
            stateHistory.forEach { appendLine(it) }
            appendLine()
            appendLine("""请严格输出 JSON：{"taskComplete":bool,"summary":string,"keyFindings":[string],"nextSuggestions":[string]} 无多余文字。""")
        }
        return system to user
    }
}
