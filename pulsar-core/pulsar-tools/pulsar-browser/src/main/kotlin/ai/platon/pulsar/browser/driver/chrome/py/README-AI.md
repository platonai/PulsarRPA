# DomService Python → Kotlin 迁移开发计划

本文档描述如何将 `pulsar-browser/py/service.py` 中的 DomService 能力迁移为 Kotlin/JVM 原生实现，确保与现有系统保持行为一致、可灰度切换与快速回滚。

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
  - 用 Python 版本采集一组“黄金样本”页面，固化为测试基线（DOM/AX/Snapshot/序列化输出）。
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
  - 选择含 iframe、shadow DOM、长列表、表单等页面，固化 Python 基线与 Kotlin 结果；字段级对比。
- 集成测试
  - CDP 直连参考：`pulsar-tests/src/test/kotlin/ai/platon/pulsar/browser/PulsarWebDriverCDPTests.kt`。
  - 上层抽象参考：`pulsar-tests/src/test/kotlin/ai/platon/pulsar/browser/PulsarWebDriverMockSite2Tests.kt`。
  - 端到端：启动无头 Chrome，走全流程：导航 → 采集 → 合并 → 序列化 → 交付。
- 回归测试
  - 针对日志中常见站点/失败栈的专项用例。

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
  - 提供兼容映射与灰度开关；金丝雀比对。

## 九、时间排期（建议）
- 第1周：M0-M2（调研、依赖落地、CDP 采集）
- 第2周：M3-M4（数据整合、滚动逻辑、哈希/XPath）
- 第3周：M5-M6（序列化、兼容层、实现切换）
- 第4周：M7-M8（性能/鲁棒性、黄金样本对齐、上线）

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
- 采集固定站点样本，保存 Python 输出（DOM/AX/Snapshot/序列化）。
- Kotlin 输出对齐后以字段级 diff 工具比较：
  - 容忍：浮点小误差、无序集合（排序后比较）。
  - 关键字段必须一致：节点层级、frame 关联、bounds、scroll/client rects、paint order。

## 附录 C：验收标准（Definition of Done）
- 功能覆盖：M0-M8 全部完成，开关可切换且默认 Kotlin。
