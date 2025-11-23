# Tasks

## 0. Prerequisites

Read root README-AI.md and `devdocs/copilot/test-guide.md` for guidelines

## docs & comments

## feature

### override suspend fun act(observe: ObserveResult): ActResult

- function call 中有一些参数需要提前传入，或者要求 LLM 留空

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

- add driver.hover(selector) ✅

## Features

- add tool: hover
- test todolist.md, `write todolist.md with 5 steps, and then replace the plan with 7 steps, all steps are mock steps for test`
- ChatModel as a primary interface for user

## Notes

