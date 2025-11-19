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

## Failed tests 🚩



## Tool Call Upgrading Automatically  🚩

1. update MiniWebDriver.kt from WebDriver.kt
2. update ToolCallExecutor
3. update ActionValidator

## Testable Tool Calls

Ensure all expressions in tool call specification can be correctly executed.

1. test against interactive-dynamic.html
2. generate kotlin expressions for each tool call
3. call `ToolCallExecutor.execute()` to execute each kotlin expression
4. check the web page to ensure the tool call is called as expected


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

## Bugs

- WebDriver.scrollBy returns a wrong value ✅
- scroll 后，highlight 绘制错误 ✅

## Features

- add tool: hover
- test todolist.md, `write todolist.md with 5 steps, and then replace the plan with 7 steps, all steps are mock steps for test`

## Notes

- 增加对 AbstractWebDriver 中所有 js 方法的测试，在PulsarWebDriverMockSiteTests中测试 🚩

