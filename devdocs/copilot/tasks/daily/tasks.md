# Tasks

## 0. Prerequisites

Read root README-AI.md and `devdocs/copilot/test-guide.md` for guidelines

## docs & comments

- generate detailed comments for DomService and ChromeCdpDomService

## feature

### refine `ClickableElementDetector`

- `ClickableElementDetectorTest` for basic tests
- `ClickableElementDetectorE2ETest` for e2e
  - use real page `interactive-dynamic.html`
  - read `interactive-dynamic.html` to design the tests
  - write tests with the same pattern with `ChromeDomServiceIsScrollableTest`

### implement ChromeCdpDomService#buildBrowserState

### When serialize object to json in pulsar-browser module, always Round to two decimal places

### override suspend fun act(observe: ObserveResult): ActResult

- function call 中有一些参数需要提前传入，或者要求 LLM 留空

### Fully locator support in WebDriver

在 PulsarWebDriver 中，对所有依赖 selector 的方法进行改进：

1. 如果 selector 具有 `backend:` 前缀，则 `backend:` 后的部分为 `backendNodeId`，使用该 id 进行元素定位。
2. 其他情况，保持现有逻辑不变，使用标准 css path 定位

### CDP scrolling methods in PulsarWebDriver

使用 CDP 方法实现 PulsarWebDriver 的 scroll 方法。

## Failed tests

ai.platon.pulsar.browser.PulsarWebDriverMockSite2Tests#When navigate to a HTML page then the navigate state are correct

ai.platon.pulsar.browser.driver.chrome.dom.ChromeDomServiceIsScrollableTest#isScrollable basics - regular elements and overflow hidden

ai.platon.pulsar.browser.driver.chrome.dom.ChromeDomServiceIsScrollableTest#isScrollable special - body html and toggle overflow

## Tool Call Upgrading Automatically

1. update MiniWebDriver.kt from WebDriver.kt
2. update ToolCallExecutor
3. update ActionValidator

## Testable Tool Calls

Ensure all expressions in tool call specification can be correctly executed.

1. test against interactive-dynamic.html
2. generate kotlin expressions for each tool call
3. call `ToolCallExecutor.execute()` to execute each kotlin expression
4. check the web page to ensure the tool call is called as expected
5.

## PageStateTracker

1. 能否避免js？
2. 能否避免全局变量？可能会被检测
3. 还有哪些实现方法？
4. 选择效率高的方法
5. 判断策略

## Agent Process Tracking

Track everything, write to file, can be restored, can be analyzed by human and by AI.

- Execution context
- Step Result
- ProcessTrace
- LLM conversation

May be combined:

- Checkpoint
- AgentState history

充分使用文件系统来保留各种现场数据，智能体需要能够随时调阅文档库。

## Notes

- if a tool call not handled by parser in ToolCallExecutor, fallback to ToolCallExecutor.eval()
- NanoDOMTreeNode 确保每一个可见元素的文档坐标均存在
- highlight 非常重要
- click() 增加 Key 参数，譬如 Ctrl + click
- 增加对 AbstractWebDriver 中所有 js 方法的测试，在PulsarWebDriverMockSiteTests中测试

