# ISSUE-135 — 为 LLM 注入 Tabs、扩展 BrowserState、新增 switchTab 工具（设计与实施计划 / 已对齐新框架）

更新时间：2025-10-25

## 背景与变更摘要
- 目标保持不变：
  1) 在发给 LLM 的消息中增补“浏览器所有 tabs 信息”；
  2) 在 BrowserState 中保存“所有 tabs 信息”；
  3) 新增工具 switchTab，允许 agent 切换 tab，且切换后 agent 的当前 driver 与之同步。
- 当前仓库已进行框架性改动（已完成）：
  - BrowserPerceptiveAgent：引入 AgenticSession，使用 `session.boundDriver` 作为“活动 driver”；`InferenceEngine(session, ...)` 由会话提供 driver；`PageStateTracker(session, ...)` 亦按会话取活跃 driver。
  - ToolCallExecutor：`TOOL_CALL_LIST` 新增了工具定义：
    - `browser.switchTab(tabId: Int): Int`
    - 注释约定：入参为 tabId（将补充到 BrowserState），返回值为 driverId，可用于定位 Browser.drivers 中的 driver。

基于以上现状，本计划已对齐新的“会话绑定式 driver 切换”框架，以下为“达成目标所需剩余工作”。

---

## 待办清单（基于新框架收敛）
1) 数据模型：扩展 BrowserState，新增 tabs/activeTabId（最小必要字段）
2) Inference 注入：在返回 BrowserUseState 前合成 tabs/activeTabId（基于 session.boundDriver 所在浏览器）
3) LLM 消息：
   - Observe 路径：无需改（BrowserState JSON 自动带 tabs）
   - Extract 路径：PromptBuilder/InferenceEngine 增加 BrowserState JSON 段
4) 工具执行：实现 `browser.switchTab(tabId: Int): Int` 的执行分支（ToolCallExecutor）
5) Agent 绑定：在 BrowserPerceptiveAgent 的工具执行路径识别 `browser.switchTab`，基于返回的 driverId 执行 `session.bindDriver(newDriver)`
6) InferenceEngine 修正：避免 domService 绑定旧 driver（改为动态获取或在切换后重建）
7) 测试：单测+集成测试（含异常用例）
8) 文档：更新 BrowserState 字段与 switchTab 用法

---

## 详细设计（对齐当前框架）

### A. 数据模型扩展（BrowserState + TabState）
- 位置：`pulsar-core/pulsar-tools/pulsar-browser/.../dom/model/DomModels.kt`
- 新增：
  - `data class TabState(
      val id: String,
      val driverId: Int? = null,
      val url: String,
      val title: String? = null,
      val active: Boolean = false,
    )`
  - `data class BrowserState(..., val tabs: List<TabState> = emptyList(), val activeTabId: String? = null)`
- 说明：
  - id 对齐浏览器内部 tabId（建议等于 `Browser.drivers` 的 key）【审核员：采纳，可修改相应字段类型】；
  - driverId 方便诊断/调试；
  - activeTabId 标记当前活动 tab（即 `session.boundDriver` 对应的 tab）。

### B. tabs 信息来源与注入（会话绑定）
- 位置：`pulsar-core/pulsar-agentic/.../InferenceEngine.kt` 或辅助类（保持 DomService 无跨层依赖）
- 策略：
  - 通过 `session.boundDriver?.browser` 获取当前浏览器上下文；
  - 从 `browser.drivers: Map<String, WebDriver>` 构建 `List<TabState>`：
    - `id = drivers.key`（String）；
    - `driverId = driver.id`；
    - `url = driver.currentUrl()`（或 `driver.url()`，二者差异见接口说明）
    - `active = (driver == session.boundDriver)`；
  - `activeTabId =` 活动 driver 对应的 map key；
  - 对 `domService.getBrowserUseState(...)` 的结果进行“补丁合成”（复制 BrowserState，填入 tabs/activeTabId），作为最终返回。
- 备注：避免改动 DomService/ChromeCdpDomService 的职责与依赖。

### C. LLM 消息注入点
- Observe：已有 `PromptBuilder.buildObserveUserMessage(...)` 注入 `browserStateJson`，扩展后自动包含 tabs。
- Extract：需要在消息里加入 BrowserState 段；
  - `PromptBuilder.buildExtractUserPrompt(...)` 增加重载或参数，追加：
    - `## Current Browser State` + `browserStateJson`
  - `InferenceEngine.extract(...)` 调用新的重载，传入 `params.browserUseState.browserState.lazyJson`。

### D. 工具执行（TOOL_CALL_LIST 已有定义）
- 位置：`pulsar-core/pulsar-skeleton/.../ToolCallExecutor.kt`
- 实现执行分支：
  - 解析到 `objectName == "browser" && functionName == "switchTab"`；
  - 取 `tabId` 参数（Int），与 `browser.drivers` 的 key 对齐：
    - 若 drivers 的 key 为 String，优先采用 `tabId.toString()` 进行匹配；
    - 找到目标 `WebDriver` 后，返回其 `driver.id`（Int）作为执行结果；
  - 找不到时，返回可读错误摘要（不抛异常，避免 LLM 进入失败循环）。
- 说明：ToolCallExecutor 不负责绑定 session，仅负责解析/定位并返回 driverId（遵循现框架职责分离）。

### E. Agent 会话绑定切换
- 位置：`pulsar-core/pulsar-agentic/.../BrowserPerceptiveAgent.kt`
- 处理逻辑：
  - 在 `doExecuteToolCall(...)` 执行后，若 `toolCall.domain == "browser" && toolCall.name == "switchTab"`：
    - 捕获 `InstructionResult`（`execute(action)` 的返回），读取其中的 `results` 列表的首个返回值；
    - 若为 `Int driverId`：通过 `val browser = requireNotNull(session.boundDriver).browser`，在 `browser.drivers.values` 中按 `driver.id == driverId` 寻找目标 `WebDriver`；
    - 若找到：`session.bindDriver(targetDriver)`，并记录历史/结构化日志；
    - 若找不到：记录失败信息，返回可读错误给 LLM（或作为 no-op 处理，视策略）。
- 注意：如 `parseStepActionResponse` 仍无法提供 domain，需以 `toolCall.name == "switchTab"` 作为兜底触发器。

### F. InferenceEngine domService 动态化（关键修正）
- 现状：`InferenceEngine` 中 `val domService: DomService = (driver as AbstractWebDriver).domService!!` 在构造期绑定 driver，切换后会“陈旧”。
- 修正方案（二选一，推荐 A）：
  - A) 改为动态 getter：`val domService get() = (session.boundDriver as AbstractWebDriver).domService!!`；
  - B) 在 `browser.switchTab` 绑定成功后，重建 `InferenceEngine(session, tta.chatModel)`（但需改写引用处，成本更高）。
- 结论：采用 A，风险更低、改动更小。

### G. 工具调用的 domain 兼容处理（重要补充）
- 现状：`BrowserPerceptiveAgent.parseStepActionResponse(...)` 中构造 `ToolCall` 时将 `domain` 硬编码为 `"driver"`，这会导致 LLM 即使选择了 `browser.switchTab(...)`，也只能解析到 `method = "switchTab"` 且 `domain = "driver"`。
- 影响：
  - `ToolCallExecutor` 若仅按 `objectName` 分路（`browser`/`driver`），则 `switchTab` 分支无法命中。
- 方案：
  1) 解析端修正（推荐）：让 `parseStepActionResponse` 能携带/还原 domain。
     - 做法：在 `buildObserveResultSchemaContract` 中明确 `method` 接受全名（如 `browser.switchTab`）；解析时若检测到 `method` 包含 `.`，拆分出 `domain` 与 `name`。

---

## 测试计划（对齐框架）
- 单元测试：
  - `DomModels` 序列化：`tabs/activeTabId` 字段存在且默认值兼容；
  - `ToolCallExecutor.switchTab`：
    - 正常路径：存在 tabId -> 返回 driverId；
    - 异常路径：不存在 tabId -> 返回可读错误（非异常）。
- 集成测试（`pulsar-tests`）：
  - 新增 `SwitchTabIntegrationTest`：
    - 打开多个页面，确保有 ≥2 个 drivers；
    - 观察 `InferenceEngine.observe/extract` 的 LLM 输入（或日志）包含 `tabs` JSON；
    - 执行 `browser.switchTab(tabId=...)`，验证：
      - `session.boundDriver` 已更新为目标 driver；
      - 随后执行一次 `driver.click()/type()` 等操作，目标 URL/driverId 符合预期；
    - 异常路径覆盖：tabId 不存在时的恢复策略。

---

## 文档更新
- `docs/rest-api-examples.md` / `docs/prompts/*`：
  - 增补 `BrowserState.tabs/activeTabId` 结构说明与样例；
  - 工具 `browser.switchTab(tabId: Int): Int` 使用说明与限制；
- `README-AI.md`：增加“多标签页感知与切换（会话绑定）”说明。

---

## 风险与缓解（更新）
- domService 陈旧：已以动态 getter 方案规避（必须落地）。
- tabId 类型与 drivers key 类型不一致（Int vs String）：先用 `tabId.toString()` 桥接；在文档中明确类型约定；如有需要可改为 `String`。
- LLM token 增长：`tabs` 字段使用最小集（id/driverId/url/active），必要时可在多 tab 时截断或摘要（后续优化）。

---

## 实施步骤（更新后）
1) DomModels：新增 `TabState`，扩展 `BrowserState`（tabs/activeTabId）
2) Inference 注入：在 `InferenceEngine` 里补丁合成 tabs/activeTabId 并返回新的 `BrowserUseState`
3) Prompt/消息：
   - `PromptBuilder.buildExtractUserPrompt(...)` 扩展以注入 `browserStateJson`
   - `InferenceEngine.extract(...)` 改为使用新重载
4) ToolCallExecutor：实现 `browser.switchTab(tabId: Int): Int` 分支（按 `tabId.toString()` 匹配 drivers key），并为 `functionName == "switchTab"` 提供兜底路由
5) Agent 切换：`BrowserPerceptiveAgent.doExecuteToolCall(...)` 识别该工具（无论 domain 是否为 `browser`），读取返回的 `driverId` 并执行 `session.bindDriver(newDriver)`
6) InferenceEngine：将 `domService` 改为动态 getter，避免陈旧引用
7) 解析修正：`parseStepActionResponse(...)` 支持 `browser.switchTab` 形式的 `method` 并拆分 domain/name（或至少保留原 method 原文以便下游拆分）
8) 测试：补齐单测与集成测试（含 domain 兼容路径）
9) 文档：更新用户与开发文档

---

## 验收标准（AC）
- AC1：`BrowserState JSON` 中包含 `tabs[]` 与 `activeTabId`；observe 与 extract 的 LLM 输入中均可见
- AC2：`browser.switchTab(tabId: Int): Int` 可用；错误路径返回可解释信息
- AC3：执行 `switchTab` 后，`session.boundDriver` 已更新，后续操作在新 driver 上执行
- AC4：编译、现有测试与新增测试全部通过；无新增高噪日志
- AC5：文档同步更新

---

## 进度标记（截至本次更新）
- 已完成：
  - BrowserPerceptiveAgent 改造为会话绑定驱动（activeDriver = session.boundDriver）；
  - InferenceEngine 构造从 session 注入；
  - TOOL_CALL_LIST 增加 `browser.switchTab(tabId: Int): Int` 定义；
- 待完成（按步骤推进）：见“实施步骤”。

---

## 后续增强（非本次必需）
- switchTab 参数扩展：`index: Int`、`urlContains: String` 等更多定位方式；
- 提供 `getTabs()` 工具用于显式拉取 tabs 列表（当 tabs 很多时按需获取，进一步节省 token）；
- 在 BrowserState 中加入更多上下文字段（如 targetId/focusedWindow 等）。

