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
- 非目标：不引入新功能，仅做必要的鲁棒性增强（超时、重试、容错）。

---

## 二、当前实现概览（更新 2025-10-13）
已落地的 Kotlin 实现（核心路径均在 `pulsar-core/pulsar-tools/pulsar-browser` 模块）：
- 服务入口
  - `chrome/dom/ChromeCdpDomService.kt`：对齐 Python DomService 的主实现，串联 DOM/AX/Snapshot 采集、增强合并、序列化与元素查找。
- CDP 处理器
  - `chrome/AccessibilityHandler.kt`：启用/采集 AX 树，支持多 frame 递归收集与选择器过滤、可滚动元素探测。
  - `chrome/dom/DomTreeHandler.kt`：获取 DOM 树并映射为增强节点，维护 backendNodeId → 节点索引。
  - `chrome/dom/DomSnapshotHandler.kt`：快照采集（样式/rect/paintOrder/stacking），增强版支持 absolute bounds 计算。
- 模型与序列化
  - `chrome/dom/model/DomModels.kt`、`DomTypes.kt`：EnhancedDOMTreeNode、EnhancedAXNode、EnhancedSnapshotNode、SimplifiedNode 等数据结构（字段名对齐 Python 语义）。
  - `chrome/dom/DomLLMSerializer.kt`：LLM 友好序列化，含 paint-order 剪枝、复合组件检测、属性大小写对齐、选择器映射。
- 工具
  - `chrome/dom/XPathUtils.kt`：跨 shadow DOM/iframe 的 XPath 生成与缓存。
  - `chrome/dom/HashUtils.kt`：元素哈希（父链/静态属性/后端节点/会话可配）、父链哈希。
  - `chrome/dom/ScrollUtils.kt`：可滚动性判定、信息文本与去重策略。

功能覆盖现状：
- DOM/AX/Snapshot 采集：已实现；AX 按 frame 聚合；Snapshot 支持样式/rect/paintOrder/stacking。
- 树合并与增强：已实现滚动性/可见性/可交互性/交互指数、XPath、哈希、绝对坐标（基础版）。
- 简化树与 LLM 序列化：已实现选择器映射与剪枝策略。
- 元素查找与交互映射：支持 elementHash/XPath/backendNodeId/CSS 选择器（基础选择器）。

---

## 三、与 Python 行为对照（要点）
- JSON 字段：模型命名与注解对齐 Python（如 `element_hash`、`x_path`、`clientRects` 等）。
- 哈希策略：支持“父链+静态属性”的传统策略与“后端节点/会话”策略，可通过 `HashUtils.HashConfig` 调整。
- XPath：考虑 shadow host、iframe 边界与同名兄弟索引，生成规则更健壮（仍保持向下兼容）。
- 滚动性：AX+样式+rect 结合，含嵌套容器去重；`iframe/body/html` 专项处理。
- 可见性/交互性：依据样式与 AX role/光标等，stacking 仅做占位处理（见“已知待办”）。

---

## 四、兼容/回滚策略
- 不提供回滚到 python 方案。

---

## 五、构建与运行（Windows 优先说明）
- 统一使用 Maven Wrapper 从项目根目录调用。
- 会自动拉起 Mock Server（见测试基类注解），无需手动启动。

示例命令（Windows cmd.exe）：
- 仅运行相关测试模块
  - `mvnw.cmd -pl pulsar-tests -am test -D"spotless.apply.skip=true"`
- 全量（所有模块）测试
  - `mvnw.cmd -Pall-modules test -D"spotless.apply.skip=true"`

Unix/macOS（参考）：
- `./mvnw -pl pulsar-tests -am test -D"spotless.apply.skip=true"`
- `./mvnw -Pall-modules test -D"spotless.apply.skip=true"`

---

## 六、测试与验证（现状与计划）
- 已有测试
  - 端到端：`pulsar-tests/src/test/kotlin/.../ChromeCdpDomServiceE2ETest.kt`
    - 骨架：`getAllTrees` → `buildEnhancedDomTree` → `buildSimplifiedTree` → `serializeForLLM`
    - 访问页面：`/generated/interactive-dynamic.html`（由 Mock Server 提供，端口 18080）。
  - 单元/组件：
    - `HashUtilsTests.kt`、`ScrollUtilsTest.kt`、`DomLLMSerializerTest.kt`（覆盖哈希、滚动、序列化关键逻辑）。
- 待补测试
  - `AccessibilityHandlerTests.kt`：AX 树结构与字段映射。
  - `DomSnapshotHandlerTests.kt`：快照样式/rect/paintOrder/stacking 与 absolute bounds。
  - 黄金样本对齐测试：将 E2E 结果落盘到 `pulsar-tests/src/test/resources/golden/domservice/<case>/current`，并与版本化基线比对（TODO：落盘工具方法与基线初始化）。

运行要点：
- Mock Server：`@SpringBootTest(classes = [EnabledMockServerApplication::class], webEnvironment = DEFINED_PORT)` 标注的测试会自动启动 `127.0.0.1:18080`，静态资源目录为 `pulsar-tests-common/src/main/resources/static/generated`。
- 页面选择：优先 `interactive-dynamic.html`，必要时使用 `interactive-1~4.html`、`interactive-screens.html`；若覆盖不足，按模板新增 `interactive-<feature>.html`（并更新路由与黄金样本）。

---

## 七、已知待办与风险
- Stacking context 遮挡与可见性：目前仅记录 `stacking_contexts` 与 `paint_order`，未完整计算遮挡关系（TODO）。
- 绝对坐标：`DomSnapshotHandler` 的 absolute bounds 计算为基础版，缺失定位上下文链/变换矩阵（可迭代增强）。
- 选择器查找：`findElement` 的 CSS 选择器仅覆盖简单情形（tag/#id/.class），复杂选择器可考虑利用 Runtime.evaluate 或 CSS domain（权衡成本）。
- 性能：XPath/哈希缓存已加入；在长页面/大量节点时需关注内存占用与序列化体积（建议压测与阈值调参）。

---

## 八、里程碑与验收（建议）
- M1：AX/DOM/Snapshot 采集与合并完成，E2E 测试可稳定通过（当前基本达成）。
- M2：完善 AX/Snapshot 单测与黄金样本落盘对齐，Stacking 可见性规则初版。
- M3：引入可选运行时开关与更多复杂选择器/遮挡计算，覆盖更多 Mock 页面与真实站点回归。

---

## 变更记录
- 2025-10-13：新增“当前实现概览/测试现状/构建与运行/已知待办与风险”等章节，E2E 用例已就绪但黄金样本落盘待补。
