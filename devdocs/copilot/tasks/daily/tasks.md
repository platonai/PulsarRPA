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

## Notes

- if a tool call not handled by parser in ToolCallExecutor, fallback to ToolCallExecutor.eval()
- NanoDOMTreeNode 确保每一个可见元素的文档坐标均存在
- highlight 非常重要
- click() 增加 Key 参数，譬如 Ctrl + click
-
