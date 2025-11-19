# BrowserPerceptiveAgent — 可交互元素高亮方案（计划）

目标
- 在 BrowserPerceptiveAgent 的 observe/act 循环中，对模型判定的“可交互元素候选”进行可视化高亮，便于调试、追踪决策与在人工接管时快速定位。
- 方案参考 human-private/session.py 的 highlight 思路；仓库未找到该 Python 文件，故结合现有前端高亮实现（pulsar-tools 中的 build_dom_tree.js/__vision_utils__.js）制定等价方案。

现有可复用能力与参考
- pulsar-core/pulsar-tools/pulsar-browser/…/build_dom_tree.js
  - highlightElement(element, index, parentIframe) 支持为元素绘制 overlay、标注序号、处理多 rect、跨 iframe 容器（HIGHLIGHT_CONTAINER_ID）。
- pulsar-core/pulsar-tools/pulsar-browser/…/__vision_utils__.js
  - 说明文档与测试脚本中有“高亮 Top N 元素并记录”的约定。
- BrowserPerceptiveAgent 当前流程位点：
  - doObserveForAct -> inference.observe -> 返回 ActionDescription.observeElement（含 locator/backendNodeId/cssFriendlyExpression 等）
  - executeToolCall 之前/之后，以及 onDidNavigateTo 导航后。

总体设计
- 高亮由注入的 JS 高亮器实现，Kotlin 侧通过 driver.evaluate/evaluateAsync 调用。尽量复用 build_dom_tree.js 的高亮能力，统一容器与样式。
- 引入 HighlightParams 配置，支持 TopN、样式、持续时间与清理策略。
- 集成点：
  1) 观察产生候选（doObserveForAct 返回）后，短暂高亮前 K 个候选（标注 1..K）。
  2) 准备执行某候选前，二次高亮“被选中”的元素（加粗边框/不同颜色），执行后进行清理。
  3) 发生导航（onDidNavigateTo）或步进到下一次 observe 时，清理旧 overlay，必要时重绘。

关键数据映射
- 元素定位来源：
  - ObserveElement.backendNodeId（优先）
  - ObserveResult.locator（CSS/XPath 或 backend: 前缀）
  - ObserveElement.cssFriendlyExpression（用于调试展示）
- JS 侧按以下优先级解析：
  1) backend:123（如果注入侧提供从 backendNodeId 查到 DOM 节点的能力；否则退化到选择器）
  2) CSS/XPath 选择器（尽量优先 CSS）
- 若解析失败则跳过该候选并在日志中标记。

Kotlin/Agent 侧改动（计划）
- AgentConfig 新增开关与参数：
  - enableHighlight: Boolean = true
  - highlightTopN: Int = 3
  - highlightStyles: data class（primaryColor, selectedColor, borderWidth, labelBg 等）
  - highlightPersistAcrossSteps: Boolean = false
  - highlightDurationMs: Long = 2500
  - clearOnNavigation: Boolean = true
  - highlightOnObserve: Boolean = true
  - highlightOnExecute: Boolean = true
- BrowserPerceptiveAgent 新增私有方法：
  - suspend fun highlightCandidates(candidates: List<ObserveResult>, selectedIndex: Int? = null, reason: String)
    - 生成 [{selector/backendId, score/index, label}] 列表
    - 调用 ensureHighlighterInstalled() -> injectHighlighterIfAbsent()
    - 调用 page 注入函数 window.__pulsarHighlighter.highlight(list, options)
  - suspend fun highlightSelected(observe: ObserveResult, reason: String)
  - suspend fun clearHighlights(reason: String)
  - suspend fun ensureHighlighterInstalled()
- 调用时机：
  - doObserveForAct(params, …) 返回后：
    - 若 enableHighlight && highlightOnObserve -> highlightCandidates(results.take(highlightTopN), null, "observe")
  - executeToolCall 之前：
    - 若 enableHighlight && highlightOnExecute -> highlightSelected(stepAction.observeElement, "pre-exec")
  - executeToolCall 成功/失败后：
    - clearHighlights("post-exec")，如需保留，延时到 highlightDurationMs 再清理
  - onDidNavigateTo：
    - 若 clearOnNavigation -> clearHighlights("navigation")

JS 注入侧（计划）
- 统一一个轻量高亮器注入到 window.__pulsarHighlighter：
  - install(): 创建容器、全局样式（尽可能复用 build_dom_tree.js 的类名与容器 ID）。
  - highlight(items, options): 根据 items 渲染 overlay 与 label（含序号），支持 pointer-events: none；对 selected 项使用不同颜色/阴影。
  - pulse(index)/select(index): 将某 index 的 overlay 强化；
  - clear(): 移除所有 overlay 容器。
- 若页面已有 Playwright/现有容器 ID（如 "playwright-highlight-container"），尽量复用容器，避免重复叠加。
- 多 iframe 支持：在 items 中携带 frame 路径或通过全局遍历 frames 定位（参考 build_dom_tree.js 的 parentIframe 处理方式）。

异常与性能
- 出错降级：JS evaluate 失败或注入异常时，仅记录日志，不影响主流程。
- 去重与节流：
  - 仅渲染前 K 个候选；
  - 同一 step 内若候选集合未变化不重复重绘（可缓存上次的 key hash）。
- 导航清理：beforeunload/unload 监听清理容器，或由 Kotlin 侧在 onDidNavigateTo 强制清理。

日志与可观测性
- 统一 StructuredAgentLogger 打点：
  - highlight.install.ok/fail, highlight.render.ok/fail, highlight.clear.ok/fail
  - 记录 step、url、候选数、selectedIndex、时延
- processTrace 中追加 meta 信息，便于 session log 回放。

边界场景
- 空候选集：跳过渲染。
- 长选择器/跨 shadow DOM：允许失败；后续可扩展为使用 backendNodeId/CDP 方案。
- 页面滚动、元素不可见：高亮器不滚动页面；后续可考虑自动滚动到视窗内。
- SPA 快速重渲染：高亮容器被移除时尝试重建。

测试（计划）
- pulsar-tests 中新增标签用例（快速 <5s）：
  1) 打开静态测试页（含多个按钮/链接），模拟 observe 输出若干候选，验证 JS 注入后 overlay 与 label 存在。
  2) 执行一个点击动作前后，验证选中态高亮与清理。
  3) 触发导航（或 hash 变化），验证清理触发。
- pulsar-tests-common 可放置静态 HTML 资源；断言通过 DOM 查询容器 ID/className。

实施步骤（迭代）
1) AgentConfig 扩展高亮开关/参数（默认开启但 TopN=0 时等价关闭）。
2) 引入/内嵌 JS 高亮器片段：
   - 优先复用 pulsar-tools 的实现片段（必要时提取到共享资源目录）；
   - 提供 ensureHighlighterInstalled()/clear()/highlight() 的桥接包装。
3) 在 doObserveForAct 与 executeToolCall 两处嵌入调用；在 onDidNavigateTo 清理。
4) 结构化日志打点与故障降级。
5) 添加 2-3 个轻量测试用例与文档。

验收标准（DoD）
- 构建与相关测试通过，无新增高噪日志。
- 开启 enableHighlight 且 highlightTopN>0 时，在 observe/act 阶段可看到前端 overlay 标注；关闭时无副作用。
- 导航后不残留旧高亮；错误不影响主流程。
- 文档与配置项说明同步更新。

后续增强
- 使用 backendNodeId/CDP 定位替代 CSS 选择器，提升准确性与跨 shadow DOM 能力。
- 支持鼠标悬停时显示候选详情（text/role/score）。
- 将高亮控制暴露为 ToolCall（如 driver.highlightOn/Off，driver.highlightSelector）。

