# DomService Python → Kotlin 迁移开发计划

本文档描述如何将 `pulsar-browser/py/service.py` 中的 DomService 能力迁移为 Kotlin/JVM 原生实现，确保与现有系统保持行为一致、可灰度切换与快速回滚。

> **说明**：Python DomService 代码目前仅作为历史文档与行为参考，不再纳入可执行路径，也不会在测试流程中被调用。

---

## 执行清单（给审阅者）
- 目标与范围达成一致
- 技术选型确认（CDP 客户端、序列化、并发）
- 接口与数据模型评审
- 迁移里程碑计划与验收标准
- 兼容/回滚策略
- 测试与基线对齐方案
- 构建与配置项
- 风险清单与缓解措施

---

## 一、目标与范围
- 目标：将 Python DomService 的核心能力（CDP 采集 DOM/AX/Snapshot、树合并、序列化、元素定位与哈希、滚动信息推断、跨 iframe/shadow DOM 处理）迁移为 Kotlin 实现，融入当前 Maven/Kotlin 构建体系。
- 范围：
  - 对外 JSON 字段与语义尽量保持不变，降低上层改动成本。
  - 使用 JVM CDP 客户端替代 Python 的 cdp_use，覆盖 Accessibility/DOM/DOMSnapshot/Target/Emulation 等域。
  - 提供实现开关（kotlin|python），阶段性回归对比。
- 非目标：不引入新功能，仅做必要的鲁棒性增强（超时、重试、容错）。

## 二、技术选型与依赖
- CDP 客户端（JVM）：
  - 明确采用：kklisura/chrome-devtools-java-client（已在项目中使用 v2023 API，覆盖 AX/DOM/DOMSnapshot/Target/Emulation/Runtime/CSS 等域）。
  - 说明：通过现有的 RemoteDevTools 封装统一获得 domain 客户端（page/dom/css/runtime/accessibility），无需额外引入其他 CDP 库。
  - 兜底：如遇个别端点缺失，可使用同库提供的原始方法调用进行 protocol 级补齐。
- 序列化：Jackson（项目已有），字段名与 Python 输出一致；必要时使用 @JsonProperty 映射。
- 并发与超时：本模块不考虑（由上层统一治理）。本实现保持串行 CDP 操作即可。
- 日志与指标：沿用 slf4j/kotlin-logging；埋点 cdp_timing、target 切换、异常原因。

## 三、公共接口与数据模型（建议包：ai.platon.pulsar.browser.driver.chrome.dom）
- 接口
  - interface DomService
    - fun getAllTrees(target: PageTarget, options: SnapshotOptions): TargetAllTrees
    - fun buildEnhancedDomTree(trees: TargetAllTrees): EnhancedDOMTreeNode
    - fun serializeForLLM(root: SimplifiedNode, includeAttributes: List<String>): String
    - fun findElement(ref: ElementRefCriteria): EnhancedDOMTreeNode?
    - fun toInteractedElement(node: EnhancedDOMTreeNode): DOMInteractedElement
- 核心数据类（字段名对齐 Python 结构）
  - NodeType, DOMRect, EnhancedAXProperty, EnhancedAXNode, EnhancedSnapshotNode
  - EnhancedDOMTreeNode（含 frameId、sessionId、shadowRoots、contentDocument）
  - SimplifiedNode（LLM 友好树）
  - TargetAllTrees（snapshot/dom_tree/ax_tree/devicePixelRatio/cdp_timing）
  - DOMInteractedElement（element_hash、x_path、bounds）
- 哈希与 XPath：
  - 复刻 Python 的 parent-branch + STATIC_ATTRIBUTES sha256 哈希；预留基于 backendNodeId+sessionId 的实现切换。
  - XPath 构造逻辑与停止边界（shadow/iframe）保持一致。

### 现有 Handler 抽象与实现计划（chrome-devtools-java-client 驱动）
- 已有抽象与类（位于 `ai.platon.pulsar.browser.driver.chrome`）：
  - `PageHandler`：封装 Page/DOM/CSS/Runtime 常用操作（导航、查询、计算样式、滚动/聚焦等）。
  - `AccessibilityHandler`：接入 Accessibility 域（AX 树获取等）。当前已存在骨架类，需完善实现。
  - 其他：`ScreenshotHandler` 等。
- 本次需要落实：
  - 完成 `AccessibilityHandler` 的实现，基于 `com.github.kklisura.cdt.protocol.v2023.types.accessibility`：
    - getFullAXTree(depth: Int?, frameId: String?): List<AXNode>
    - 按 frameId 获取指定 Frame 的 AX 树；与 DOM/backendNodeId 做后续关联。
    - 预留 selector → AXNode 过滤的入口（必要时通过 Runtime.evaluate 实现定位后再用 AX 过滤）。
  - 如有需要，新增轻量 Handler 抽象层，职责单一、面向 DomService：
    - `DomTreeHandler`：封装 DOM.getDocument / describeNode / querySelector / attributes 等纯 DOM 能力。
    - `DomSnapshotHandler`：封装 DOMSnapshot.captureSnapshot 及解析（样式/布局/scroll/client/bounds/paint order）。
  - 以上 Handler 以 `RemoteDevTools` 为依赖入口，统一从中获取 domain 客户端实例。

## 四、功能分解与里程碑
- M0 调研与基线
  - 梳理 `service.py`/`views.py` 的职责与输出形状，形成 ADR（接口输入/输出、域调用、边界条件）。
  - 以 Kotlin 实现采集一组“黄金样本”页面，固化为测试基线（DOM/AX/Snapshot/序列化输出），Python 结果仅供行为理解。
- M1 CDP 会话与 Target 管理
  - 建立 Chrome 连接、Target 创建/附加、Session 管理（PageTarget 与 iframe TargetInfo 映射）。
  - 获取 devicePixelRatio、基本导航、加载等待。
  - Handler 层落地：`PageHandler` 复用；补齐 `AccessibilityHandler`；按需增加 `DomTreeHandler`、`DomSnapshotHandler`。
- M2 树采集层（与 Python 等价的 CDP 调用）
  - Accessibility.getFullAXTree（由 `AccessibilityHandler` 提供）。
  - DOM.getDocument（含 depth 管理、frame/iframe 关联，建议由 `DomTreeHandler` 提供）。
  - DOMSnapshot.captureSnapshot（样式、布局、scroll/client/bounds、paint order，建议由 `DomSnapshotHandler` 提供）。
  - 统计 cdp_timing 指标。
- M3 数据整合层
  - 将三棵树合并为 EnhancedDOMTreeNode（nodeId/backendNodeId 对齐；AX 关联；snapshot 属性灌入）。
  - 处理 iframe 的 contentDocument、shadowRoots 合并。
  - 支持通过 SnapshotOptions 关闭/开启昂贵字段的采集。
- M4 功能增强层
  - 滚动性判断 is_actually_scrollable（基于 snapshot 的 scrollRects/clientRects/computed_styles）。
  - should_show_scroll_info 与 get_scroll_info_text 逻辑对齐。
  - XPath、element_hash、parent_branch_hash 的 Kotlin 复刻。
- M5 LLM 序列化与选择器映射
  - 构造 SimplifiedNode，避免 children_nodes/shadow_roots 重复序列化。
  - 实现 serializeForLLM（对齐 Python DOMTreeSerializer 的 include_attributes 逻辑）。
  - 构建 DOMSelectorMap（element_hash → node）。
- M6 兼容层与实现切换
  - Kotlin 实现的 Facade 暴露与 Python DomService 等价的调用表。
  - application.properties 增加开关，允许回退到 Python 实现。
- M7 性能与鲁棒性
  - 大页面的内存/时间优化（限深/限宽、可选域、延迟装配）。
  - 断开/超时/Target detaching 的重试与幂等（注意：并发/超时仍由上层控制，本模块只保证顺序执行）。
- M8 对齐验证与上线
  - 与“黄金样本”字段级 diff（允许浮点/顺序容忍阈值），达到一致性目标。
  - 文档与开关收尾，上线灰度。

## 五、兼容性与回滚
- 外部接口：保持 JSON 字段与结构不变；必要时提供旧字段兼容映射。
- 回滚：提供实现开关；失败时日志提示 + 自动降级到 Python。
- 跨平台：使用 Maven Wrapper 构建；确保 Windows/macOS/Linux 一致行为。

## 六、测试与验证
- 单元测试（位置：`pulsar-tests/src/test/kotlin/ai/platon/pulsar/browser`）
  - 数据类序列化/反序列化；哈希/XPath；滚动信息边界。
  - Handler 层单测建议：
    - `AccessibilityHandlerTests.kt`：验证 getFullAXTree 的返回结构与字段映射。
    - `DomSnapshotHandlerTests.kt`：验证 captureSnapshot 输出关键信息（scroll/client/bounds）。
- 基准（Golden）测试
  - 选择含 iframe、shadow DOM、长列表、表单等页面，固化 Kotlin 基线；按版本化 diff 验证字段级一致性，Python 结果仅用于行为参照。
- 集成测试
  - CDP 直连参考：`pulsar-tests/src/test/kotlin/ai/platon/pulsar/browser/PulsarWebDriverCDPTests.kt`。
  - 上层抽象参考：`pulsar-tests/src/test/kotlin/ai/platon/pulsar/browser/PulsarWebDriverMockSite2Tests.kt`。
  - 端到端：启动无头 Chrome，走全流程：导航 → 采集 → 合并 → 序列化 → 交付。
- 回归测试
  - 针对日志中常见站点/失败栈的专项用例。

### 端到端测试设计（更新 2025/10/13）
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
  ./mvnw -pl pulsar-tests-common spring-boot:run -Dspring-boot.run.main-class=ai.platon.pulsar.util.server.EnabledMockServerApplication -Dspring-boot.run.profiles=test
  ```
  该命令在当前仓库根目录运行，启动后可直接访问 `http://127.0.0.1:18080/generated/interactive-dynamic.html` 等页面。

#### 测试网页构造
- 现有 Mock Server 已提供所需的静态与动态页面（位于 `pulsar-tests-common/src/main/resources/static/generated` 目录及其生成内容），默认使用 `interactive-dynamic.html` 作为黄金样本入口。
- 若 `interactive-dynamic.html` 无法满足特定场景，需在 `static/generated` 中甄别 `interactive-*.html` 是否涵盖该能力，并在文档中标注选择原因。
- 若仍无法满足，用 `interactive-dynamic.html` 的结构（异步加载、列表编辑、虚拟滚动等模块）为模板新增页面，命名为 `interactive-<feature>.html`，并更新 MockSiteController 暴露路由，同时将页面加入版本化黄金样本目录。

## 七、构建交付与配置
- 依赖
  - 在 pulsar-browser 模块引入 JVM CDP 客户端依赖（已采用 chrome-devtools-java-client）；Jackson。
  - 如需新增 Handler，请与现有 `RemoteDevTools` 对齐，避免重复管理连接。
- 配置项（application.properties 示例）
  - pulsar.dom.includeAttributes
  - pulsar.dom.snapshot.timeout
  - pulsar.dom.maxDepth / maxChildren
  - pulsar.dom.impl=kotlin|python
- 构建与运行（建议）
  - Windows: 使用 mvnw.cmd 从项目根目录构建/测试
  - *nix: 使用 ./mvnw 构建/测试

## 八、风险与缓解
- CDP 域覆盖差异或语义不一致
  - 采用覆盖 DOMSnapshot/AX 的 chrome-devtools-java-client；不足处以同库原始 Protocol 调用兜底。
- 性能与内存压力
  - 限深/限宽；可配置域；延迟构建；压测与统计。
- 跨 iframe/shadow DOM 对齐难
  - 以复杂页面建立黄金样本，严格字段对齐。
- 输出细微差异影响上层
  - 提供兼容映射与灰度开关。

- **M0 调研与基线**：Kotlin 接口与模型已落地（参见 `dom/DomService.kt`、`dom/model`）；Kotlin 黄金样本仍未固化，需要端到端测试采集脚本。
- **M1 会话与 Handler 层**：`AccessibilityHandler`、`DomTreeHandler`、`DomSnapshotHandler` 已实现并接入 `RemoteDevTools`；尚缺多 target/断线回退场景测试。
- **M2 树采集层**：`ChromeCdpDomService.getAllTrees` 可抓取 DOM/AX/Snapshot 并统计 `cdpTiming`；需在多 frame 页面验证性能与容错。
- **M3 合并层**：`buildEnhancedDomTree` 支持滚动、可见性、交互指标合并；但 stacking context 仍存在 TODO，缺乏自动化验证。
- **M4 功能增强**：滚动与交互性计算已迁移，Shadow DOM/XPath/element hash 实现同步；依旧需要针对嵌套滚动与 iframe 场景的回归测试。
- **M5 序列化层**：`DomLLMSerializer` 已提供 paint-order pruning、selector map 多键映射；尚未在真实数据上做 diff。
- **M6 实现切换**：配置开关尚未统一到 `application*.properties`，仅在代码层缓存最近一次结果；需要灰度策略文档化。
- **M7 性能与鲁棒性**：尚无压测或 retry 策略验证，待端到端测试覆盖大页面。
- **M8 对齐验证**：缺少 Kotlin 黄金基线 diff 管道，CI 未接入验证流程。

## 十、产出物清单
- 接口与实现
  - ai/platon/pulsar/browser/driver/chrome/dom/DomService.kt
  - ai/platon/pulsar/browser/driver/chrome/dom/ChromeCdpDomService.kt
  - ai/platon/pulsar/browser/driver/chrome/dom/model/*.kt（数据类/枚举/序列化）
  - ai/platon/pulsar/browser/driver/chrome/dom/serializer/*.kt（LLM 序列化）
  - ai/platon/pulsar/browser/driver/chrome/AccessibilityHandler.kt（完成 AX 能力）
  - ai/platon/pulsar/browser/driver/chrome/DomSnapshotHandler.kt（若新增）
  - ai/platon/pulsar/browser/driver/chrome/DomTreeHandler.kt（若新增）
- 配置与文档
  - application.properties 示例项与 README 更新
  - ADR：迁移对齐说明与差异清单
- 测试
  - 单测：哈希/XPath/滚动信息/序列化/Handler
  - 集成：端到端抓取与比对（参考 CDP 与 MockSite2 测试）
  - 基准：黄金样本与 diff 工具

## 附录 A：Python 视图模型要点（来自 views.py）
- NodeType、DOMRect、EnhancedAXNode、EnhancedSnapshotNode、EnhancedDOMTreeNode、SimplifiedNode、DOMInteractedElement 结构与行为是 Kotlin 映射基准。
- element_hash：基于 parent-branch 路径 + STATIC_ATTRIBUTES 的 sha256；需在 Kotlin 复刻并保留后续切换到 backendNodeId+sessionId 的选项。
- XPath：
  - 生成时遇到 shadow root 透传；遇到 iframe 停止；同标签兄弟用 1-based 索引。
- 滚动信息：
  - is_actually_scrollable：scrollRects vs clientRects + CSS overflow 判断；iframe/body/html 特判；嵌套滚动去重。
  - get_scroll_info_text：对 iframe 与普通元素采用不同的简洁呈现。

## 附录 B：黄金样本与差异对比方法
- 采集固定站点样本，保存 Kotlin 输出（DOM/AX/Snapshot/序列化），必要时记录 Python 参考结果用于诊断。
- Kotlin 输出对齐后以字段级 diff 工具比较：
  - 容忍：浮点小误差、无序集合（排序后比较）。
  - 关键字段必须一致：节点层级、frame 关联、bounds、scroll/client rects、paint order。

## 附录 C：验收标准（Definition of Done）
- 功能覆盖：M0-M8 全部完成，开关可切换且默认 Kotlin。

---

## 下一步开发计划（2025/10/13）

1. **端到端黄金样本基线**
  - 按“端到端测试设计”章节实现采集脚本与资源目录，固化 Kotlin DomService 输出；补充 `README` 与脚本使用说明。
    - 将 `TestWebSiteAccess.interactiveUrl` 与相关端到端测试更新为 `interactive-dynamic.html`，覆盖基础/Shadow DOM/iframe/滚动四类页面，记录加载参数与超时策略，并生成首批黄金数据（必要时备注 Python 行为差异，仅供诊断）。

2. **Kotlin 黄金 Diff 流水线**
  - 编写 `DomServiceGoldenComparator`，差异按 `major|minor|meta` 分类，并在失败时输出结构化报告。
  - 在 `DomServiceEndToEndTests.kt` 中调用 comparator，确保 Kotlin 实现与黄金基线对齐。

3. **CI 与灰度集成**
  - 在 `pulsar-tests` 引入 @Tag 控制端到端用例，新增 Maven profile `dom-e2e`，将测试纳入夜间/变更触发流水线。
  - 配置 `application*.properties` 的 `pulsar.dom.impl` 开关与回退策略文档，使 CI 报告可链接到当前实现模式。

4. **验证缺口补强**
  - 针对 stacking context、滚动去重、可见性判断补充针对性用例，并将指标采集写入 diff 报告。
  - 对照黄金样本结果，整理 TODO 列表（包含多 target 断线回退、性能数据），作为后续迭代 backlog。
