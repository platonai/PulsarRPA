# DomService 测试

### 测试设计
- **目标**：在统一的测试框架内验证 Kotlin `ChromeCdpDomService` 的端到端流程，输出可回放的结构化结果，并对比版本化的 Kotlin 黄金基线。
- **测试骨架**：在 `pulsar-tests` 新增 `DomServiceEndToEndTests.kt`（JUnit5 @Tag("dom-e2e")），复用 `RemoteDevToolsRule` 启动无头 Chrome。测试步骤：
  1. 使用 Kotlin 实现依次调用 `getAllTrees` → `buildEnhancedDomTree` → `buildSimplifiedTree` → `serializeForLLM`，将结果写入 `pulsar-tests/src/test/resources/golden/domservice/<case>/current`。
  2. 引入版本化的基线目录 `.../baseline`（由已验证的 Kotlin DomService 生成），测试时与当前输出对比。
  3. 利用共享 diff 工具（新增 `DomServiceGoldenComparator") 对关键字段（节点计数、frame 关联、bounds/scrollRects、AX role/name、LLM JSON）做容差比较。
- **样本库**：首批覆盖四类页面 —— 基础静态、Shadow DOM、Nested iframe、长列表/滚动。样本 URL 与加载参数记录在 `docs/copilot/domservice-e2e-cases.md`，并通过脚本 `bin/script-tests/domservice-golden.ps1` 可重采集；Python DomService 输出仅用于分析差异成因，不参与断言。
- **Mock 页面选择**：遵循 `pulsar-tests/src/test/kotlin/ai/platon/pulsar/browser/PulsarWebDriverMockSite2Tests.kt` 的模式，所有端到端用例均通过 Mock Server（`http://127.0.0.1:18080`）访问 `static/generated` 目录下的交互页面：
  - Mock Server 启动后，所有 `static` 目录下的网页可以直接访问。
  - 首选 `GET /generated/interactive-dynamic.html`，并将 `TestWebSiteAccess.interactiveUrl` 指向该资源后复用 `runWebDriverTest` 的调用方式，以覆盖延迟加载、滚动、虚拟列队等高频场景；
  - 若某能力在 `interactive-dynamic.html` 中无法复现，依次验证 `interactive-1.html` ~ `interactive-4.html`、`interactive-screens.html` 等资源，记录差异并将所选页面纳入黄金样本；
  - 若现有页面仍不足以覆盖需求，则以 `interactive-dynamic.html` 为模板在 `static/generated` 下新增页面（命名为 `interactive-*.html`）。
  - 支撑性校验（纯文本/JSON）可继续复用 `GET /hello`、`/json` 等轻量接口。
- **断言与指标**：
  - 所有差异分类为 `major`（结构）、`minor`（浮点/顺序）、`meta`（计时）。CI 仅在 `major`/`minor` 时失败。
  - 度量 `cdpTiming`、节点数、生成耗时，输出到 `logs/chat-model/domservice-e2e.json` 便于回归分析。
- **运行方式**：
  - 本地：`./mvnw -pl pulsar-tests -am test -Dtest=DomServiceEndToEndTests -Dgroups=dom-e2e`。
  - CI：按 tag 控制在夜间构建或变更触发运行，黄金差异通过 artifact 附件暴露。

#### Mock Server 启动方式
- Mock Server 基于 `ai.platon.pulsar.util.server.EnabledMockServerApplication`，监听 `server.port=18080`（定义于 `pulsar-tests-common/src/main/resources/config/application-test.properties`）。
- **自动模式**：运行任何标注 `@SpringBootTest(classes = [EnabledMockServerApplication::class], webEnvironment = DEFINED_PORT)` 的测试时（包括 `DomServiceEndToEndTests`），Spring Boot 会自动拉起 Mock Server 并在测试结束后关闭。
- **手动模式**（若需单独调试）：执行
  ```powershell
  ./mvnw -pl pulsar-tests-common spring-boot:run -D"spring-boot.run.main-class=ai.platon.pulsar.util.server.EnabledMockServerApplication" -D"spring-boot.run.profiles=test"
  ```
  该命令在当前仓库根目录运行，启动后可直接访问 `http://127.0.0.1:18080/generated/interactive-dynamic.html` 等页面。

#### 测试网页构造
- 现有 Mock Server 已提供所需的静态与动态页面（位于 `pulsar-tests-common/src/main/resources/static/generated` 目录及其生成内容），默认使用 `interactive-dynamic.html` 作为黄金样本入口。
- 若 `interactive-dynamic.html` 无法满足特定场景，需在 `static/generated` 中甄别 `interactive-*.html` 是否涵盖该能力，并在文档中标注选择原因。
- 若仍无法满足，用 `interactive-dynamic.html` 的结构（异步加载、列表编辑、虚拟滚动等模块）为模板新增页面，命名为 `interactive-<feature>.html`，并更新 MockSiteController 暴露路由，同时将页面加入版本化黄金样本目录。
