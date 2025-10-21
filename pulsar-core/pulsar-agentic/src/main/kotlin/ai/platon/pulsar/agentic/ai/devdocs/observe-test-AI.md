# PerceptiveAgent.extract / observe 端到端测试指南

## 📋 Prerequisites
在开始之前，请先快速阅读：
- 根目录 `README-AI.md`（全局开发规范与项目结构）
- 了解本工程为多模块 Maven 项目，主要语言为 Kotlin

## 🎯 目标与范围
- 目标：在统一的端到端测试框架内，覆盖并验证 `PerceptiveAgent.extract()` 与 `PerceptiveAgent.observe()` 的核心行为与稳健性。
- 代码位置：`pulsar-core/pulsar-skeleton/src/main/kotlin/ai/platon/pulsar/skeleton/ai/agent/PulsarAgentImpl.kt`
- 关键类型：`PerceptiveAgent`, `PulsarAgentImpl`, `InferenceEngine`, `DomService`
- 参考现有 E2E 模式：`pulsar-tests/src/test/kotlin/ai/platon/pulsar/browser/driver/chrome/dom/ChromeDomServiceE2ETest.kt`

## 🧩 测试样本与覆盖面
- 首批覆盖页面类型：基础静态、Shadow DOM、Nested iframe、长列表/滚动。
- Mock 资源路径：`pulsar-tests-common/src/main/resources/static/generated`（测试时通过 Mock Server 以 `http://127.0.0.1:18080/generated/...` 访问）。
- 默认入口页：`/generated/interactive-dynamic.html`（优先使用）。如该页无法复现特定能力，再尝试 `interactive-1.html` ~ `interactive-4.html`、`interactive-screens.html` 等。
- 若仍不足以覆盖需求，可在 `static/generated` 下以 `interactive-dynamic.html` 为模板新增 `interactive-*.html` 页面，并纳入黄金样本。

## 🧪 测试骨架与推荐结构
- 测试模块：`pulsar-tests`
- 基类：继承 `WebDriverTestBase`，可获取 `generatedAssetsBaseURL` 以及 `runWebDriverTest(url) { driver -> ... }` 便捷方法。
- 简要约定：
  - 通过 `runWebDriverTest(testURL)` 启动浏览器与导航；
  - 在回调中构造 `PulsarAgentImpl(driver)`；
  - 针对不同页面调用 `observe()` 与 `extract()`，并断言输出结构与基本质量；
  - 度量并记录 `cdpTiming`、节点数、JSON 大小、耗时等指标，输出到 `logs/chat-model/domservice-e2e.json`。

> 说明：现有 `ChromeDomServiceE2ETest` 已展示了树构建与 LLM 序列化的端到端路径，可直接复用其计量与写入逻辑（按需调整）。

## ✅ 断言与指标（差异分级）
- 差异分级：
  - major：结构性错误（例如关键字段缺失/类型错误/空结果）
  - minor：浮点偏差、顺序等非结构性差异
  - meta：计时/统计等元信息差异
- CI 仅在出现 major/minor 时失败；meta 只用于回归分析。
- 建议断言：
  - observe：返回列表非空；每项包含非空 `description`；如有 `method/arguments`，结构合法；`selector` 合法（非空/格式合理）。
  - extract：`success == true`；`data` 非空且含预期字段；对默认/自定义 schema 的兼容性验证。
  - 公共：序列化产物长度、DOM 节点数、AX 节点数、生成时间等在合理范围。

## ▶ 运行方式
- Mock Server：基于 `ai.platon.pulsar.util.server.EnabledMockServerApplication`，当测试类标注了 `@SpringBootTest(classes = [EnabledMockServerApplication::class], webEnvironment = DEFINED_PORT)` 或继承了 `WebDriverTestBase` 时，将自动在固定端口启动（默认 18080），测试结束后关闭。
- Windows/cmd 本地运行（推荐使用 Maven Wrapper）：
  - 运行现有 DomService E2E 用例：
    - `mvnw.cmd -pl pulsar-tests -am -Dtest=ai.platon.pulsar.browser.driver.chrome.dom.ChromeDomServiceE2ETest test`
  - 运行新增的 Agent E2E 用例（示例类名，创建后替换为真实包名）：
    - `mvnw.cmd -pl pulsar-tests -am -Dtest=ai.platon.pulsar.skeleton.ai.agent.PulsarAgentExtractObserveE2ETest test`

> 注：若需选择性地只跑某个测试方法，可使用 `-Dtest=ClassName#methodName` 形式。
> LLM 配置说明：`PerceptiveAgent.*` 相关 E2E 依赖 LLM。若未配置 API Key（见 `docs/config/llm/llm-config.md`），测试会自动跳过（Assumption）。

## 🧱 最小可行 E2E 示例（建议参考实现）
以下为构造 Agent 并调用 `observe`/`extract` 的建议结构（放置于 `pulsar-tests` 模块中，包名可根据实际需要调整）。

- 输入：测试 URL（优先 `interactive-dynamic.html`）
- 输出：
  - observe：返回的候选元素列表（非空）；
  - extract：返回的结构化数据（`success==true` 且含关键字段）。
- 错误模式：网络/超时/无元素/空数据等。
- 成功标准：断言通过且指标记录成功写入 `logs/chat-model/domservice-e2e.json`。

示意步骤：
1) `runWebDriverTest(testURL) { driver -> ... }`
2) `val agent = PulsarAgentImpl(driver)`
3) `val observed = agent.observe("Understand the page and list actionable elements")`
   - 断言：`observed.isNotEmpty()`、`observed.all { it.description.isNotBlank() }`
4) `val extracted = agent.extract("Extract key structured data from the page")`
   - 断言：`extracted.success` 且 `extracted.data` 含预期键值
5) 记录指标：URL、时间戳、节点/快照计数、JSON 大小、差异分级（默认 meta）等

> 可直接复用 `ChromeDomServiceE2ETest` 中的 JSON 追加写入与计数逻辑，修改 case 名称与数据来源。

## 🌐 页面与路由补充
- 访问前缀：`http://127.0.0.1:18080`。
- 常用路径：
  - `GET /generated/interactive-dynamic.html`（默认优先）
  - `GET /generated/interactive-1.html` ~ `interactive-4.html`
  - `GET /generated/interactive-screens.html`
- 新增页面时：在 `static/generated` 下新增 `interactive-*.html`，并确保资源被打包到 `pulsar-tests-common` 的测试资源中；一般静态资源会被自动暴露为路径，无需额外路由；如需特定 JSON/CSV 文本接口，可参考 `pulsar-tests/src/main/kotlin/ai/platon/pulsar/test/mock2/server/MockSiteController.kt`。

## 🔁 回归与黄金样本
- 将选定页面纳入黄金样本集合，定期（或在关键变更后）重采集并对比。
- 记录差异并分级（major/minor/meta），仅在 major/minor 时中断 CI。
- 指标文件：`logs/chat-model/domservice-e2e.json`（每行一条 JSON，便于增量追踪）。

## 📎 参考文件
- Agent 实现：`pulsar-core/pulsar-skeleton/.../PulsarAgentImpl.kt`
- DomService E2E：`pulsar-tests/.../ChromeDomServiceE2ETest.kt`
- WebDriver 基类：`pulsar-tests/.../WebDriverTestBase.kt`
- Mock Server 应用：`pulsar-tests/.../EnabledMockServerApplication.kt`

—— 本文档已对齐仓库中实际文件命名与路径，提供可直接落地的运行与编写指引。
