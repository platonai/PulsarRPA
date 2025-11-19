# BrowserPerceptiveAgent 写入 todo.md 方案（v2025-11-06）

—

## 1) 目标与原则
- 将 BrowserPerceptiveAgent 的运行过程与结果以“计划+进度”的方式落地到 `todo.md`。
- 遵循人类私有规范文档（human-private/todo.md）的建议：
  - 仅针对多步骤/长期任务写入；短小任务避免引入额外开销。
  - `todo.md` 作为过程/进度的单一事实来源（SSOT），`results.md` 聚焦结果。
- 最小改动、低噪日志、可配置、可回退；失败不影响主流程。

## 2) 新增配置（AgentConfig）
- `enableTodoWrites: Boolean = true`
- `todoPlanWithLLM: Boolean = false`  // 默认不用 LLM 自动生成计划，保持最小改动
- `todoWriteProgressEveryStep: Boolean = true`
- `todoProgressWriteEveryNSteps: Int = 1`  // 配合上面使用
- `todoMaxProgressLines: Int = 200`
- `todoEnableAutoCheck: Boolean = true`
- `todoTagsFromToolCall: Boolean = true`

说明：默认即可启用“每步进度写入+自动勾选（启发式）”，如需更低噪可将 `todoProgressWriteEveryNSteps` 调大。

## 3) `todo.md` 文件结构（建议）
- 头部元信息
  - `# TODO for session <short-uuid>`
  - `Instruction: <精简>`, `Started at: <时间>`, `Current URL: <url>`
  - `Progress: (0/N)` 或 `Progress: (0/∞)`（无预定义计划时）
- 计划（Plan）
  - 当 `todoPlanWithLLM=false`：提供占位说明（Plan TBD），引导在执行过程中补全计划。
  - 当 `todoPlanWithLLM=true`：生成 5–10 个可落地步骤（含标签）。
  - 计划项格式：`- [ ] Step x: <说明>  #action:<method> #domain:<host>`
- 进度（Progress Log）
  - 每次成功动作追加一行：`- [OK] HH:mm:ss <method> "<selectorSnippet>" @ <url> | <summary>`
- 备注（Notes）
  - 关键异常、分支决策等。

## 4) 代码改动点（`BrowserPerceptiveAgent`）
在 `pulsar-core/pulsar-agentic/.../ai/BrowserPerceptiveAgent.kt` 中新增若干私有方法并在关键节点调用：

- 初始化：在 `doResolveProblem()` 开头（拿到 `instruction`/当前 URL 后）
  - `primeTodoIfEmpty(instruction, url)`：若 `todo.md` 为空则写入头部、空计划段、空进度段与备注段占位。

- 每步成功：在 `executeToolCall()` 成功分支后
  - 按频率控制（`todoWriteProgressEveryStep` 或 `step % todoProgressWriteEveryNSteps == 0`）
    - `appendTodoProgress(step, toolCall, observe, url, summary)`：追加一条 `[OK]` 记录，保持上限 `todoMaxProgressLines`。
    - `updateProgressCounter()`：将头部的 `(k/N)` 或 `(k/∞)` 中 k 递增。
    - `markPlanItemDoneByTags(tags)`（若启用）：基于 method、domain 等标签，尝试将匹配计划项从 `- [ ]` 改为 `- [x]`。

- 任务完成：在 `handleTaskCompletion()`
  - 追加完成标记；确保 Progress 计数与最后一步对齐。
  - 可在 `persistTranscript()` 前后各写一次轻量标记，避免遗漏。

### 4.1 新增私有方法（示意）
- `suspend fun primeTodoIfEmpty(instruction: String)`
  - 读取 `fs.getTodoContents()`；若空则 `fs.writeString("todo.md", <初始内容>)`。
- `suspend fun appendTodoProgress(step, toolCall, observe, url, summary)`
  - 构造一行进度文本并 `fs.append("todo.md", line)`。
  - 超过 `todoMaxProgressLines` 时停止追加或做轻量截断策略（可选）。
- `suspend fun updateProgressCounter()`
  - `fs.replaceContent("todo.md", "(k/N)", "(k+1/N)")` 或 `k/∞` 变体。
- `suspend fun markPlanItemDoneByTags(tags: Set<String>)`
  - 读取全文，定位“Plan”段；将第一条“未勾选且包含任一标签”的计划项替换为已勾选。
- `fun buildTags(toolCall, url): Set<String>`
  - 生成 `#action:<method>`、`#domain:<host>`，必要时短截 `#selector:<hint>`。
- `fun selectorSnippet(observe): String`
  - 从 `ObserveElement` 取 css 或 backendId 的简短可读片段。

## 5) 勾选策略（启发式）
- 计划项在行尾包含标签：`#action:<method>`、`#domain:<host>`。
- 执行动作派生标签，优先匹配同 action；若 domain 也匹配则优先级更高。
- 命中第一条未勾选计划项即替换 `- [ ]` -> `- [x]`。
- 若无匹配，不强行勾选，仅更新 Progress 计数。

## 6) 幂等与容错
- 所有 `fs.*` 写入包裹 `runCatching {}`；失败写结构化日志，不中断主流程。
- 写入频率可配置；默认每步写，必要时调大 `todoProgressWriteEveryNSteps`。
- 使用较具体的 `replaceContent` 片段以减少误替换。

## 7) 测试计划
- 单测（`pulsar-tests-common`）
  - `primeTodoIfEmpty`：空 -> 初始化后包含头部与各段占位。
  - `appendTodoProgress`：两次成功写入 -> 存在两条 `[OK]` 且 Progress 计数递增。
  - `markPlanItemDoneByTags`：预置一条 `#action:click` 计划项，执行一次 click -> 行内 `- [ ]` 变 `- [x]`。
- 轻量集成（mock driver）
  - 构造伪 `ActionDescription/ToolCall`，调用上述方法，验证文本格式与替换正确性。
- 验收标准
  - 构建与相关测试通过；默认不开 LLM 计划也稳定运行。
  - 无高噪日志；写入失败不影响 agent 主逻辑。

## 8) 实施步骤（推荐分批提交）
1) 最小可用（MVP）
   - 新增配置项；实现 `primeTodoIfEmpty` / `appendTodoProgress` / `updateProgressCounter`。
   - 在 `doResolveProblem`、`executeToolCall` 成功分支与 `handleTaskCompletion` 挂接。
   - 单测覆盖三条主路径。
2) 增强勾选
   - 实现标签生成与 `markPlanItemDoneByTags`。
   - 计划区增加 2–3 条占位项（示例标签）。
3) 可选：LLM 计划生成与回退
   - 开启 `todoPlanWithLLM` 后，生成 5–10 步计划，失败回退到占位计划。

## 9) 风险与回退
- 过度写入导致 IO/日志开销：通过 `todoProgressWriteEveryNSteps` 限流。
- `replaceContent` 误匹配：限定匹配片段包含标签与固定前缀。
- 任务过短不适用：默认只要启用开关即写；可按需判断步数阈值再启用（后续增强）。
- 回退策略：关闭 `enableTodoWrites` 或将频率提升到较大步长，即可快速停止或降低影响。

## 10) `todo.md` 样例（片段）
```
# TODO for session 9af31c2b
Instruction: 抓取并分析示例站点的搜索结果
Started at: 2025-11-06T10:23:18Z
Current URL: https://example.com
Progress: (0/∞)

## Plan
- [ ] Step 1: 打开目标站点主页  #action:navigateTo #domain:example.com
- [ ] Step 2: 在搜索框输入关键词并提交  #action:type #action:click #domain:example.com
- [ ] Step 3: 翻页并采集结果摘要  #action:scrollDown #action:click #domain:example.com

## Progress Log
- [OK] 10:23:21 navigateTo "https://example.com" @ https://example.com | ✅ navigateTo executed successfully
- [OK] 10:23:25 type "#search" @ https://example.com | ✅ type executed successfully

## Notes
- 若页面元素不可见，先滚动再点击。
```

—

附：实现时遵循 `docs/log-format.md` 的结构化日志，写入失败仅告警，不中断流程；所有路径通过 `FileSystem` 统一读写（`writeString`/`append`/`replaceContent`/`getTodoContents`）。
