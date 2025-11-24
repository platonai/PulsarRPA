package ai.platon.pulsar.agentic.ai

import ai.platon.pulsar.agentic.AgentHistory
import ai.platon.pulsar.agentic.ai.agent.ExtractParams
import ai.platon.pulsar.agentic.ai.agent.ObserveParams
import ai.platon.pulsar.agentic.ai.agent.detail.ExecutionContext
import ai.platon.pulsar.agentic.tools.ToolSpecification
import ai.platon.pulsar.browser.driver.chrome.dom.DOMSerializer
import ai.platon.pulsar.browser.driver.chrome.dom.model.TabState
import ai.platon.pulsar.common.KStrings
import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.ai.llm.PromptTemplate
import ai.platon.pulsar.common.brief
import ai.platon.pulsar.common.serialize.json.Pson
import ai.platon.pulsar.common.serialize.json.pulsarObjectMapper
import ai.platon.pulsar.agentic.AgentState
import java.time.LocalDate
import java.util.*

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

        fun buildObserveResultSchema(returnAction: Boolean): String {
            // English is better for LLM to understand json
            val schema1 = """
{
  "elements": [
    {
      "locator": "Web page node locator, composed of two numbers, such as `0,4`",
      "description": "Description of the current locator and tool selection",
      "domain": "Tool domain, such as `driver`",
      "method": "Method name, such as `click`",
      "arguments": [
        {
          "name": "Parameter name, such as `selector`",
          "value": "Parameter value, such as `0,4`"
        }
      ],
      "screenshotContentSummary": "Summary of the current screenshot content",
      "currentPageContentSummary": "Summary of the current web page text content, based on the accessibility tree or web content extraction results",
      "memory": "1–3 specific sentences describing this step and the overall progress. This should include information helpful for future progress tracking, such as the number of pages visited or items found.",
      "thinking": "A structured <think>-style reasoning block that applies the `## 推理规则`.",
      "evaluationPreviousGoal": "A concise one-sentence analysis of the previous action, clearly stating success, failure, or uncertainty.",
      "nextGoal": "A clear one-sentence statement of the next direct goal and action to take."
    }
  ]
}
"""

            val schema2 = """
{
  "elements": [
    {
      "locator": string,
      "description": string
    }
  ]
}
""".let { Strings.compactWhitespaces(it) }

            return if (returnAction) schema1 else schema2
        }

        val TASK_COMPLETE_SCHEMA = """
            {"taskComplete":bool,"success":bool,"errorCause":string?,"summary":string,"keyFindings":[string],"nextSuggestions":[string]}
        """.trimIndent()

        val TOOL_CALL_RULE_CONTENT = """
严格遵循以下规则使用浏览器和浏览网页：

- domain: 方法域，如 driver, browser 等
- 输出结果中，定位节点时 `selector` 字段始终填入 `locator` 的值，不提供不能确定的参数
- 确保 `locator` 与对应的可交互元素列表中的 `locator` 完全匹配，或者与无障碍树节点属性完全匹配，准确定位该节点
- JSON 格式输出时，禁止包含任何额外文本
- 从`## 浏览器状态`段落获得所有打开标签页的信息
- 如需检索信息，新建标签页而非复用当前页
- 使用 `click(selector, "Ctrl")` 新建标签页，在**新标签页**打开链接。系统若为 macOS，自动将 Ctrl 映射为 Meta
- 如果目标页面在**新标签页**打开，使用 `browser.switchTab(tabId: String)` 切换到目标页面，从`## 浏览器状态`段落获得 `tabId`
- 按键操作（如"按回车"），用press方法（参数为"A"/"Enter"/"Space"）。特殊键首字母大写。不要模拟点击屏幕键盘上的按键
- 仅对特殊按键（如 Enter、Tab、Escape）进行首字母大写
- 注意：用户难以区分按钮和链接
- 若预期元素缺失，尝试刷新页面、滚动或返回上一页
- 若向字段输入内容：1. 无需先滚动和聚焦（工具内部处理）2. 可能需1) 回车 2) 显式搜索按钮 3) 下拉选项以完成操作。
- 若填写输入框后操作序列中断，通常是因为页面发生了变化（例如输入框下方弹出了建议选项）
- 若出现验证码，尽可能尝试解决；若无法解决，则启用备用策略（例如换其他站点、回退上一步）
- 若页面因输入文本等操作发生变化，需判断是否要交互新出现的元素（例如从列表中选择正确选项）。
- 若上一步操作序列因页面变化而中断，需补全未执行的剩余操作。例如，若你尝试输入文本并点击搜索按钮，但点击未执行（因页面变化），应在下一步重试点击操作。
- 始终考虑最终目标：<user_request>包含的内容。若用户指定了明确步骤，这些步骤始终具有最高优先级。
- 若<user_request>中包含具体页面信息（如商品类型、评分、价格、地点等），尝试使用筛选功能以提高效率。
- 如无必要，不要登录页面。没有凭证时，绝对不要尝试登录。
- 始终先判断任务属于两类哪一种：
    1. 非常具体的逐步指令
       - 精确地遵循这些步骤，不要跳过，尽力完成每一项要求。
    2. 开放式任务：
       - 自行规划并有创造性地完成任务。
       - 如果你在开放式任务中被卡住（例如遇到登录或验证码），可以重新评估任务并尝试替代方案，例如有时即使出现登录弹窗，页面的某些部分仍可访问，或者可以通过网络搜索获得信息。

    """.trimIndent()

        val EXTRACTION_TOOL_NOTE_CONTENT_2 = """
使用 `agent.extract` 满足高级数据提取要求，仅当 `textContent`, `selectFirstTextOrNull` 不能满足要求时使用。

参数说明：

1. `instruction`: 准确描述 1. 数据提取目标 2. 数据提取要求
2. `schema`: 数据提取结果的 schema 要求，以 JSON 格式描述，并且遵循下面结构
3. instruction 负责『做什么』，schema 负责『输出形状』；出现冲突时以 schema 为准

Schema 参数结构：
```
class ExtractionField(
    val name: String,
    val type: String = "string",                 // JSON schema primitive or 'object' / 'array'
    val description: String,
    val required: Boolean = true,
    val objectMemberProperties: List<ExtractionField> = emptyList(), // define the schema of member properties if type == object
    val arrayElements: ExtractionField? = null                    // define the schema of elements if type == array
)
class ExtractionSchema(val fields: List<ExtractionField>)
```

例：
```
{
  "fields": [
    {
      "name": "product",
      "type": "object",
      "description": "Product info",
      "objectMemberProperties": [
        {
          "name": "name",
          "type": "string",
          "description": "Product name",
          "required": true
        },
        {
          "name": "variants",
          "type": "array",
          "required": false,
          "arrayElements": {
            "name": "variant",
            "type": "object",
            "required": false,
            "objectMemberProperties": [
              { "name": "sku", "type": "string", "required": false },
              { "name": "price", "type": "number", "required": false }
            ]
          }
        }
      ]
    }
  ]
}
```

"""

        val EXTRACTION_TOOL_NOTE_CONTENT = EXTRACTION_TOOL_NOTE_CONTENT_2

        val INTERACTIVE_ELEMENT_LIST_NOTE_CONTENT = """
(Interactive Elements)

可交互元素列表包含页面 DOM 可交互元素的主要信息，包括元素简化 HTML 表示，文本内容，前后文本，所在视口，坐标和大小等。

列表格式：
[locator]{viewport}(x,y,width,height)<slimNode>textContent</slimNode>Text-Before-This-Interactive-Element-And-After-Previous-Interactive-Element

- 默认列出当前焦点视口，第1，2视口和最后一视口元素。
- 节点唯一定位符 `locator` 由两个整数组成，不含括号，同无障碍树保持一致。
- `viewport` 为节点所在视口序号，1-based，不含括号。
- 注意：网页内容变化可能导致视口位置随时发生变化。
- `x,y,width,height` 为节点坐标和尺寸。


        """.trimIndent()

        val A11Y_TREE_NOTE_CONTENT = """
(Accessibility Tree)

无障碍树包含页面 DOM 关键节点的主要信息，包括节点文本内容，可见性，可交互性，坐标和尺寸等。

- 除非特别指定，无障碍树仅包含网页当前视口内的节点信息，并包含少量视口外节点，以保证信息充分。
- 节点唯一定位符 `locator` 由两个整数组成。
- 对所有节点：`invisible` 默认为 `false`，`scrollable` 默认为 `false`, `interactive` 默认为 `false`。
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
5. 在智能体循环中高效运行
6. 高效地执行各类网页任务

---

## 语言设置

- 默认工作语言：**$language**
- 始终以与用户请求相同的语言回复

---

## 输入

在每一步，你的输入将包括：
1. `## 智能体历史`：按时间顺序的事件流，包含你之前的动作及其结果。
2. `## 智能体状态`：当前的 <user_request>、<file_system> 摘要、<todo_contents> 和 <agent_history> 摘要。
3. `## 浏览器状态`：当前 URL、打开的标签页、可交互元素的索引及可见页面内容。
4. `## 视觉信息`：浏览器截图。如果你之前使用过截图，这里将包含截图。

---

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

```
${ToolSpecification.TOOL_CALL_SPECIFICATION}
```

$TOOL_CALL_RULE_CONTENT

### 数据提取工具说明

$EXTRACTION_TOOL_NOTE_CONTENT

---

## 可交互元素说明

$INTERACTIVE_ELEMENT_LIST_NOTE_CONTENT

---

## 无障碍树说明

$A11Y_TREE_NOTE_CONTENT

---

## 文件系统

- 你可以访问一个持久化的文件系统，用于跟踪进度、存储结果和管理长期任务。
- 文件系统已初始化一个 `todolist.md`：用于保存已知子任务的核对清单。每当你完成一项时，优先使用 `fs.replaceContent` 工具更新 `todolist.md` 中的标记。对于长期任务，这个文件应指导你的逐步执行。
- 如果你要写入 CSV 文件，请注意当单元格内容包含逗号时使用双引号。
- 若文件过大，你只会得到预览；必要时使用 `fs.readString` 查看完整内容。
- 若任务非常长，请初始化一个 `results.md` 文件来汇总结果。
- 若需长期状态记忆，可将 memory 内容写入 fs。
- 若你判断可在 5 分钟内完成，且无需跨页面汇总，则跳过文件系统！

---

## 上步输出

- 上一步操作的输出结果

---

## 任务完成规则

你必须在以下三种情况之一结束任务，按照`任务完成输出`格式要求输出相应 json 格式：
- 当你已完全完成 USER REQUEST。
- 当达到允许的最大步骤数（`max_steps`）时，即使任务未完成也要完成。
- 如果绝对无法继续，也要完成。

`任务完成输出` 是你终止任务并与用户共享发现结果的机会。
- 仅当完整地、无缺失地完成 USER REQUEST 时，将 `success` 设为 `true`。
- 如果有任何部分缺失、不完整或不确定，将 `success` 设为 `false`，并在 summary 字段中明确说明状态。
- 如果用户要求特定格式（例如：“返回具有以下结构的 JSON”或“以指定格式返回列表”），确保在回答中使用正确的格式。
- 如果用户要求结构化输出，`## 输出格式` 段落规定的 schema 将被修改。解决任务时必须考虑该 schema。

---

## 动作规则

- 在每一步中你允许使用最多 $MAX_ACTIONS 个动作。
  - 如果允许多个动作，明确多个动作按顺序执行（一个接一个）。
- 如果页面在动作后发生了改变，序列会被中断并返回新的状态。

---

## 效率指南

- 如需输入，直接输入，无需点击、滚动或聚焦，工具层处理
- 屏幕阅读规则,默认逐屏阅读，屏幕视觉内容是推理的最终依据
- 当视口数超过5屏时，除非用户要求，否则不要逐屏阅读，而是滚动到网页底部保证网页完全加载，然后使用全文提取工具`driver.textContent()`提取网页内容进行分析
- 不要在一步中尝试多条不同路径。始终为每一步设定一个明确目标。重要的是在下一步你能看到动作是否成功，因此不要链式调用会多次改变浏览器状态的动作，例如：
   - 不要使用 click 然后再 navigateTo，因为你无法确认 click 是否成功。
   - 不要连续使用 switchTab，因为你看不到中间状态。
   - 不要使用 input 然后立即 scroll，因为你无法验证 input 是否生效。

---

## 推理规则

在每一步的 `thinking` 块中，你必须明确且系统化地进行推理。

### 推理模式

为成功完成 `<user_request>` 请遵循以下推理模式：

```
<thinking>
[1] 目标分析: 明确当前子目标与总体任务的关系。
[2] 状态评估: 检查当前页面状态、截图与上一步执行结果。
[3] 事实依据: 仅依据视觉信息、页面结构与过往记录。
[4] 问题识别: 找出阻碍任务进展的原因。
[5] 策略规划: 制定下一步最小可行行动。
</thinking>
```

---

### 推理指南

- 基于 <agent_history> 推理，以追踪朝向 <user_request> 的进展与上下文。
- 分析 <agent_history> 中最近的 `nextGoal` 与 `evaluationPreviousGoal`，并明确说明你之前尝试达成的目标。
- 分析所有相关的 <agent_history>、<browser_state> 和截图以了解当前状态。
- 明确判断上一步动作的成功/失败/不确定性。不要仅仅因为上一步在 <agent_history> 中显示已执行就认为成功。例如，你可能记录了 “动作 1/1：在元素 3 中输入 '2025-05-05'”，但输入实际上可能失败。始终使用 <browser_vision>（截图）作为主要事实依据；如果截图不可用，则备选使用 <browser_state>。若预期变化缺失，请将上一步标记为失败（或不确定），并制定恢复计划。
- 如果 `todolist.md` 为空且任务是多步的，使用文件工具在 `todolist.md` 中生成分步计划。
- 分析 `todolist.md` 以指导并追踪进展。
- 如果有任何 `todolist.md` 项已完成，请在文件中将其标记为完成。
- 分析你是否陷入了重复无进展的状态；若是，考虑替代方法，例如滚动以获取更多上下文、使用发送键（`press`）直接模拟按键，或换用不同页面。
- 决定应存储在记忆中的简明、可操作的上下文以供后续推理使用。
- 在准备结束时，按<output_done_tag />格式输出。
- 始终关注 <user_request>。仔细分析所需的具体步骤和信息，例如特定筛选条件、表单字段等，确保当前轨迹与用户请求一致。

---

## 容错行为

- 如果上一步工具调用内部出现异常，该异常会在 `## 上步输出` 中显示

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

## 输出格式

- 输出严格使用下面两种 JSON 格式之一
- 仅输出 JSON 内容，无多余文字

1. 动作输出格式
<output_act_tag />

- 最多一个元素
- arguments 必须按工具方法声明顺序排列

${buildObserveResultSchema(true)}

2. 任务完成输出格式:
<output_done_tag />

$TASK_COMPLETE_SCHEMA

---

## 安全要求：
- 仅操作可见的交互元素
- 遇到验证码或安全提示时停止执行

---

        """.trimIndent()

        const val SINGLE_ACTION_GENERATION_PROMPT = """
根据动作描述和网页内容，选择最合适一个或多个工具。

## 动作描述

{{ACTION_DESCRIPTIONS}}

---

## 工具列表

```kotlin
{{TOOL_CALL_SPECIFICATION}}
```

---

## 网页内容

网页内容以无障碍树的形式呈现:

{{NANO_TREE_LAZY_JSON}}

---

## 输出

- 仅输出 JSON 内容，无多余文字
- domain 取值 driver
- method 和 arguments 遵循 `## 工具列表` 的函数表达式

动作输出格式：
{{OUTPUT_SCHEMA_ACT}}

---

        """

        val OBSERVE_GUIDE_OUTPUT_SCHEMA = """
{
  "elements": [
    {
      "locator": "Web page node locator, composed of two numbers, such as `0,4`",
      "description": "Description of the current locator and tool selection",
      "screenshotContentSummary": "Summary of the current screenshot content",
      "currentPageContentSummary": "Summary of the current web page text content, based on the accessibility tree or web content extraction results",
      "memory": "1–3 specific sentences describing this step and the overall progress. This should include information helpful for future progress tracking, such as the number of pages visited or items found.",
      "thinking": "A structured <think>-style reasoning block that applies the `## 推理规则`."
    }
  ]
}
        """.trimIndent()

        val OBSERVE_GUIDE_OUTPUT_SCHEMA_RETURN_ACTIONS = """
{
  "elements": [
    {
      "locator": "Web page node locator, composed of two numbers, such as `0,4`",
      "description": "Description of the current locator and tool selection",
      "domain": "Tool domain, such as `driver`",
      "method": "Method name, such as `click`",
      "arguments": [
        {
          "name": "Parameter name, such as `selector`",
          "value": "Parameter value, such as `0,4`"
        }
      ],
      "screenshotContentSummary": "Summary of the current screenshot content",
      "currentPageContentSummary": "Summary of the current web page text content, based on the accessibility tree or web content extraction results",
      "memory": "1–3 specific sentences describing this step and the overall progress. This should include information helpful for future progress tracking, such as the number of pages visited or items found.",
      "thinking": "A structured <think>-style reasoning block that applies the `## 推理规则`."
    }
  ]
}
        """.trimIndent()

        val OBSERVE_GUIDE_SYSTEM_MESSAGE = """
## 总体要求

你正在通过根据用户希望观察的页面内容来查找元素，帮助用户实现浏览器操作自动化。
你将获得：
- 一条关于待观察元素的指令
- 一个包含网页所有可交互元素信息的列表
- 一个展示页面语义结构的分层无障碍树（accessibility tree）。该树是DOM（文档对象模型）与无障碍树的混合体。

如果存在符合指令的元素，则返回这些元素的数组；否则返回空数组。

---

## 浏览器状态说明

浏览器状态包括：
- 当前 URL：你当前查看页面的 URL。
- 打开的标签页：带有 id 的打开标签页。

---

## 视觉信息说明

- 如果你之前使用过截图，你将获得当前页面的截图。
- 视觉信息是首要事实依据（GROUND TRUTH）：在推理中利用图像来评估你的进展。
- 当不确定或想获取更多信息时使用截图。

---

## 可交互元素说明

$INTERACTIVE_ELEMENT_LIST_NOTE_CONTENT

---

## 无障碍树说明

$A11Y_TREE_NOTE_CONTENT

---

## 工具列表

```
${ToolSpecification.TOOL_CALL_SPECIFICATION}
```

$TOOL_CALL_RULE_CONTENT

---

## 输出格式
(<output_act_tag />)

- 输出严格使用下面 JSON 格式，仅输出 JSON 内容，无多余文字
- 最多一个元素，domain & method 字段不得为空

{{OUTPUT_SCHEMA_PLACEHOLDER}}

---

"""

        fun compactPrompt(prompt: String, maxWidth: Int = 200): String {
            val boundaries = """
你正在通过根据用户希望观察的页面内容来查找元素
否则返回空数组。

## 工具列表说明
---

## 无障碍树说明
---
            """.trimIndent()

            val boundaryPairs = boundaries.split("\n").filter { it.isNotBlank() }.chunked(2).map { it[0] to it[1] }

            val compacted = KStrings.replaceContentInSections(prompt, boundaryPairs, "\n...\n\n")

            return Strings.compactInline(compacted, maxWidth)
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

    fun buildResolveMessageListAll(context: ExecutionContext): AgentMessageList {
        // Prepare messages for model
        val messages = AgentMessageList()

        initObserveUserInstruction(context.instruction, messages)

        buildResolveMessageListStart(context, context.stateHistory, messages)

        // browser state, viewport info, interactive elements, DOM
        buildObserveUserMessageLast(messages, context)

        return messages
    }

    fun buildObserveMessageListAll(params: ObserveParams, context: ExecutionContext): AgentMessageList {
        // Prepare messages for model
        val messages = AgentMessageList()

        // observe guide
        buildObserveGuideSystemPrompt(messages, params)
        // browser state, viewport info, interactive elements, DOM
        buildObserveUserMessageLast(messages, context)

        return messages
    }

    fun buildResolveMessageListStart(
        context: ExecutionContext, stateHistory: AgentHistory,
        messages: AgentMessageList,
    ): AgentMessageList {
        val instruction = context.instruction

        val systemMsg = buildOperatorSystemPrompt()

        messages.addSystem(systemMsg)
        messages.addLastIfAbsent("user", buildUserRequestMessage(instruction), name = "user_request")
        messages.addUser(buildAgentStateHistoryMessage(stateHistory))
        if (context.screenshotB64 != null) {
            messages.addUser(buildBrowserVisionInfo())
        }

        val prevTCResult = context.agentState.prevState?.toolCallResult
        if (prevTCResult != null) {
            messages.addUser(buildPrevToolCallResultMessage(context))
        }

        return messages
    }

    fun buildObserveGuideSystemExtraPrompt(userProvidedInstructions: String?): SimpleMessage? {
        if (userProvidedInstructions.isNullOrBlank()) return null

        val contentCN = """
## 用户自定义指令

在执行操作时请牢记用户的指令。如果这些指令与当前任务无关，请忽略。

用户指令：
$userProvidedInstructions

---

""".trim()

        val contentEN = contentCN

        val content = if (isZH) contentCN else contentEN

        return SimpleMessage("system", content)
    }

    fun buildExtractSystemPrompt(userProvidedInstructions: String? = null): SimpleMessage {
        val userInstructions = buildObserveGuideSystemExtraPrompt(userProvidedInstructions)

        val content = """
# 系统指南

你正在代表用户提取内容。如果用户要求你提取“列表”信息或“全部”信息，你必须提取用户请求的所有信息。

你将获得：
1. 一条指令
2. 一个要从中提取内容的 DOM 元素列表

- 从 DOM 元素中原样打印精确文本，包含所有符号、字符和换行。
- 如果没有发现新的信息，打印 null 或空字符串。

$userInstructions

"""

        return SimpleMessage(role = "system", content = content)
    }

    fun buildAgentStateHistoryMessage(agentHistory: AgentHistory): String {
        val history = agentHistory.states
        if (history.isEmpty()) {
            return ""
        }

        val headingSize = 2
        val tailingSize = 8
        val totalSize = headingSize + tailingSize
        val result = when {
            history.size <= totalSize -> history
            else -> history.take(headingSize) + history.takeLast(tailingSize)
        }

        fun compactAgentState(agentState: AgentState): AgentState {
            return agentState.copy(
                instruction = Strings.compactInline(agentState.instruction, 20)
            )
        }

        val historyJsonList = result
            .map { compactAgentState(it) }
            .joinToString("\n") { pulsarObjectMapper().writeValueAsString(it) }

        val msg = """
## 智能体历史
(仅保留 $totalSize 步骤)

<agent_history>
$historyJsonList
</agent_history>

---

		""".trimIndent()

        return msg
    }

    fun buildAgentStateMessage(state: AgentState): String {
        val message = """
## 智能体状态

当前的 <user_request>、<file_system> 摘要、<todo_contents> 和 <agent_history> 摘要。

---

        """.trimIndent()

        return message
    }

    fun buildBrowserVisionInfo(): String {
        val visionInfo = """
## 视觉信息
<browser_vision>

- 在推理中利用图像来评估你的进展。
- 当不确定或想获取更多信息时使用截图。

[Current page screenshot provided as base64 image]

---

""".trimIndent()

        return visionInfo
    }

    fun buildPrevToolCallResultMessage(context: ExecutionContext): String {
        val agentState = requireNotNull(context.agentState)
        val toolCallResult = requireNotNull(context.agentState.prevState?.toolCallResult)
        val evaluate = toolCallResult.evaluate
        val evalResult = evaluate?.value?.toString()
        val exception = evaluate?.exception?.cause
        val evalMessage = when {
            exception != null -> "[执行异常]\n" + exception.brief()
            evalResult.isNullOrBlank() -> "[执行成功]"
            else -> "[执行成功] 输出结果：$evalResult"
        }.let { Strings.compactInline(it, 5000) }
        val help = evaluate?.exception?.help?.takeIf { it.isNotBlank() }
        val helpMessage = help?.let { "帮助信息：\n```\n$it\n```" } ?: ""
        val lastModelError = agentState.actionDescription?.modelResponse?.modelError
        val lastModelMessage = if (lastModelError != null) {
            """
上步模型错误：

$lastModelError

        """
        } else ""

        return """
## 上步输出

上步操作：${agentState.prevState?.method}
上步期望结果：${agentState.prevState?.nextGoal}

上步执行结果：
```
$evalMessage
```

$helpMessage
$lastModelMessage
---
        """.trimIndent()
    }

    fun buildUserRequestMessage(userRequest: String): String {
        val msg = """
# 当前任务

## 用户输入
<user_request>

$userRequest

---

                """.trimIndent()

        return msg
    }

    fun initExtractUserInstruction(instruction: String? = null): String {
        if (instruction.isNullOrBlank()) {
            return """
从网页中提取关键数据结构。

- 每次提供一个视口高度(viewport height)内的所有无障碍树 DOM 节点，你的数据来源是无障碍树
- 视口之上的数据视为已被处理，视口之下的数据视为待处理
- 视口之上像素高度: 当前视口上方、已滚动出可视范围的网页内容高度
- 视口之下像素高度: 当前视口下方、不在可视范围内的网页内容高度

""".trimIndent()
        }

        return instruction
    }

    fun buildExtractUserRequestPrompt(params: ExtractParams): String {
        return """
## 用户指令
<user_request>
${params.instruction}
</user_request>
        """.trimIndent()
    }

    fun buildExtractUserPrompt(params: ExtractParams): SimpleMessage {
        val browserState = params.agentState.browserUseState.browserState

        val scrollState = browserState.scrollState
        // Height in pixels of the page area above the current viewport. (被隐藏在视口上方的部分的高度)
        val hiddenTopHeight = scrollState.hiddenTopHeight
        val hiddenBottomHeight = scrollState.hiddenBottomHeight
        val viewportHeight = scrollState.viewportHeight
        val domState = params.agentState.browserUseState.domState

        // The 1-based viewport to see.
        val processingViewport = scrollState.processingViewport
        val viewportsTotal = scrollState.viewportsTotal

        val startY = scrollState.y.coerceAtLeast(0.0)
        val endY = (scrollState.y + viewportHeight).coerceAtLeast(0.0)
        val nanoTree = domState.microTree.toNanoTreeInRange(startY, endY)

        val schema = params.schema

        val content = """
## 视口信息

本次焦点视口序号: $processingViewport
视口高度：$viewportHeight
估算视口总数: $viewportsTotal
视口之上像素高度: $hiddenTopHeight
视口之下像素高度: $hiddenBottomHeight

---

## 无障碍树
（仅当前视口范围内）
${nanoTree.lazyJson}

---

## 输出
你必须返回一个严格符合以下JSON Schema的有效JSON对象。不要包含任何额外说明。

${schema.toJsonSchema()}

        """.trimIndent()

        return SimpleMessage(role = "user", content = content)
    }

    fun buildMetadataSystemPrompt(): SimpleMessage {
        val metadataSystemPromptCN: String = """
你是一名 AI 助手，负责评估一次抽取任务的进展和完成状态。

- 每次提取当前视口范围内的数据
- 视口之上的数据已处理，视口之下的数据待处理

请分析抽取响应，判断任务是否已经完成或是否需要更多信息。
严格遵循以下标准：
1. 一旦当前抽取响应已经满足了指令，必须将完成状态设为 true 并停止处理，不论是否还有未查看视口。
2. 只有在以下两个条件同时成立时，才将完成状态设为 false：
   - 指令尚未被满足
   - 仍然有剩余视口数据未提取（viewportsTotal > processingViewport）

""".trimIndent()

        return SimpleMessage(
            role = "system",
            content = metadataSystemPromptCN,
        )
    }

    fun buildMetadataUserPrompt(
        instruction: String,
        extractionResponse: Any,
        agentState: AgentState,
    ): SimpleMessage {
        /**
         * The 1-based next chunk to see, each chunk is a viewport height.
         * */
        val browserUseState = agentState.browserUseState
        val scrollState = browserUseState.browserState.scrollState
        // Height in pixels of the page area above the current viewport. (被隐藏在视口上方的部分的高度)
        val hiddenTopHeight = scrollState.hiddenTopHeight
        val hiddenBottomHeight = scrollState.hiddenBottomHeight
        val viewportHeight = scrollState.viewportHeight

        // The 1-based viewport to see.
        val processingViewport = scrollState.processingViewport
        val viewportsTotal = scrollState.viewportsTotal
        val nextViewportToSee = 1 + processingViewport

        val extractedJson = DOMSerializer.MAPPER.writeValueAsString(extractionResponse)

        val content =
            """
## 用户指令
（数据提取的最初要求）
<user_request>
$instruction
</user_request>

## 视口信息

本次焦点视口序号: $processingViewport
视口高度：$viewportHeight
估算视口总数: $viewportsTotal
视口之上像素高度: $hiddenTopHeight
视口之下像素高度: $hiddenBottomHeight

- 每次提供一个视口高度(viewport height)内的所有无障碍树 DOM 节点，你的数据来源是无障碍树
- 视口之上的数据视为已被处理，视口之下的数据视为待处理
- 视口之上像素高度: 当前视口上方、已滚动出可视范围的网页内容高度
- 视口之下像素高度: 当前视口下方、不在可视范围内的网页内容高度

---

## 提取结果

$extractedJson

---

""".trim()

        return SimpleMessage(role = "user", content = content)
    }

    private fun buildObserveGuideSystemPrompt(messages: AgentMessageList, params: ObserveParams) {
        val schema =
            if (params.returnAction) OBSERVE_GUIDE_OUTPUT_SCHEMA_RETURN_ACTIONS else OBSERVE_GUIDE_OUTPUT_SCHEMA

        val observeSystemPrompt = PromptTemplate(OBSERVE_GUIDE_SYSTEM_MESSAGE).render(
            mapOf("OUTPUT_SCHEMA_PLACEHOLDER" to schema)
        )

        messages.addLast("system", observeSystemPrompt)

        val extra = buildObserveGuideSystemExtraPrompt(params.userProvidedInstructions)?.content
        if (extra != null) {
            messages.addLast("system", extra)
        }
    }

    fun initObserveUserInstruction(instruction: String?, messages: AgentMessageList = AgentMessageList()): AgentMessageList {
        val instruction2 = when {
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

        messages.addUser(instruction2, name = "user_request")
        return messages
    }

    private fun buildObserveUserMessageLast(messages: AgentMessageList, context: ExecutionContext) {
        val prevBrowserState = context.agentState.prevState?.browserUseState?.browserState
        val browserState = context.agentState.browserUseState.browserState

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
        val hiddenTopHeight = scrollState.hiddenTopHeight
        val hiddenBottomHeight = scrollState.hiddenBottomHeight
        val viewportHeight = scrollState.viewportHeight
        val domState = context.agentState.browserUseState.domState

        // The 1-based viewport to see.
        val processingViewport = scrollState.processingViewport
        val viewportsTotal = scrollState.viewportsTotal

        val interactiveElements = context.agentState.browserUseState.getInteractiveElements()

        val delta = viewportHeight * 0.5
        val startY = (scrollState.y - delta).coerceAtLeast(0.0)
        val endY = (scrollState.y + viewportHeight + delta).coerceAtLeast(0.0)
        val nanoTree = domState.microTree.toNanoTreeInRange(startY, endY)

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
- 视口之上像素高度: 当前视口上方、已滚动出可视范围的网页内容高度。
- 视口之下像素高度: 当前视口下方、不在可视范围内的网页内容高度。
- 注意：网页内容变化可能导致视口位置和视口序号随时发生变化。
- 默认提供的无障碍树仅包含第`i`个视口内的 DOM 节点，并包含少量视口外邻近节点，以保证信息完整
- 如需查看下一视口，调用 `scrollBy(viewportHeight)` 向下滚动一屏获取更多信息

## 可交互元素

聚焦第${processingViewport}视口可交互元素。

${interactiveElements.lazyString}

## 无障碍树

聚焦第${processingViewport}视口节点。

```json
${nanoTree.lazyJson}
```

---

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
## 用户输入

根据以下动作选择一个工具来执行该动作：$action。查找动作、工具和目标最相关的页面元素。分析执行后的影响和预期结果。

---

"""

        return instruction
    }

    fun buildSummaryPrompt(goal: String, stateHistory: AgentHistory): Pair<String, String> {
        val system = "你是总结助理，请基于执行轨迹对原始目标进行总结，输出 JSON。"

//        val history = stateHistory.withIndex().joinToString("\n") {
//            "${it.index}.\t🚩 ${it.value}"
//        }

        val history = stateHistory.states.joinToString("\n") { Pson.toJson(it) }

        val user = """
## 原始目标
$goal

---

## 执行轨迹（按序）

$history

---

## 输出

严格输出 JSON，无多余文字：

$TASK_COMPLETE_SCHEMA

---

        """.trimIndent()

        return system to user
    }

    fun tr(text: String) = translate(text)

    /**
     * Translate to another language, reserved
     * */
    fun translate(text: String): String {
        return text
    }
}
