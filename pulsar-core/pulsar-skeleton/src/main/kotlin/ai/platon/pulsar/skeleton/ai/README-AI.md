# 🚦 PulsarAgent Developer Guide

## 📋 Prerequisites

Before starting development, ensure you understand:

1. **Root Directory** `README-AI.md` - Global development guidelines and project structure
2. **Project Architecture** - Multi-module Maven project with Kotlin as primary language
3. **Testing Guidelines** - Comprehensive testing strategy with unit, integration, and E2E tests
   - Unless explicitly required, web page access during testing must be directed to the Mock Server
   - The relevant web page resources are located in the directory `pulsar-tests-common/src/main/resources/static/generated/tta`

## 🎯 Overview

[PulsarAgent.kt](PulsarAgent.kt) is an **enterprise-grade multi-round planning executor** that enables AI models to perform
web automation through screenshot observation and historical action analysis. It plans and executes atomic
actions step-by-step until the target is achieved.

### Key Architecture Principles

- **Atomic Actions**: Each step performs exactly one atomic action (single click, single input, single selection)
- **Multi-round Planning**: AI model plans next action based on screenshot + action history
- **Structured Output**: Model returns JSON-formatted function calls
- **Termination Control**: Loop ends via `taskComplete=true`
- **Result Summarization**: Final summary generated using `operatorSummarySchema`
- **Error Resilience**: Graceful handling of failures with fallback strategies
- **Safety First**: URL validation and secure execution environment

## 🧪 Testing Strategy

### Integration Tests
- **Real browser automation** with Spring context

### Test Coverage Areas
1. **Action Execution Pipeline** - All tool calls (navigation, interactions, scrolling, screenshots)


## 📌 Plan: TTA → WebDriver correctness (文档/API/测试同步计划)

目标：确保 AI 能将自然语言稳定转为可执行的 WebDriver 操作；对齐文档、工具清单与实现；补齐测试覆盖。

- 基线现状
  - TextToAction 已暴露工具并映射到 driver：navigateTo, waitForSelector, click, fill, press, check/uncheck, scrollDown/Up/Top/Bottom/Middle, scrollToScreen, clickTextMatches, clickMatches, clickNthAnchor, captureScreenshot(可带 selector), delay, stop。
  - MiniWebDriver 里还有大量能力未作为 tool_call 暴露（如 exists/isVisible/focus/scrollTo(selector)/waitForNavigation 等）。
  - 测试网页集中在 `pulsar-tests-common/.../static/generated/tta`，建议所有 TTA 测试只依赖此 Mock Server。

- 优先暴露给 AI 的新工具（第一批，提升稳健性与可观测性）
  1) exists(selector), isVisible(selector)
  2) focus(selector)
  3) scrollTo(selector)
  4) waitForNavigation(oldUrl? = "", timeoutMillis? = policy)

- 次优先（第二批，增强复杂交互）
  - goBack(), goForward()
  - type(selector, text)
  - mouseWheelDown/Up(...), moveMouseTo(x,y)/moveMouseTo(selector,dx,dy), dragAndDrop(selector,dx,dy)
  - outerHTML()/outerHTML(selector)
  - selectText/Attributes/Property/Hyperlinks/Images 系列
  - getCookies(), url()/currentUrl()/documentURI()/baseURI()/referrer(), bringToFront()

- 工具调用协议（Prompt 规则）
  - 仅返回 JSON；不可猜测 selector；歧义或无把握时返回空 `{"tool_calls":[]}`。
  - 优先使用“交互元素列表”中的 selector；点击前可选 waitForSelector 与 exists/isVisible 进行防御性校验。
  - 导航型动作后建议调用 waitForNavigation。

- 元素抽取与上下文
  - 当前抽取覆盖 input/textarea/select/button/a[href] 与显式 onclick/contenteditable/role=button|link（过滤 hidden/disabled）。
  - 暂不支持 iframe/Shadow DOM；作为后续项在专用页面落地后再扩展抽取脚本与测试。
  - 定位优先级建议：data-testid > aria-label > role+name > 文本 > 相对位置。

- 文档与实现对齐事项
  - 更新工具清单（TOOL_CALL_LIST）以包含已实现但未暴露的项（已完成：scrollToScreen、clickTextMatches、clickMatches、clickNthAnchor）。
  - 在 `pulsar-tests/.../tta/README-AI.md` 标注：选择/提取类 API 目前未作为 tool_call 暴露，避免误导（已完成）。
  - 本文件加入统一计划（本节）。

- 测试计划
  - 为“第一批”新增工具各自添加最小可复现用例（Mock 页面 + 集成测试）。
  - 增加“条件动作/点击后等待跳转”等现实场景用例。
  - Mock 页面扩展：动态/歧义、a11y 元数据、长列表/多屏；后续再加 iframe/Shadow DOM 页面。
  - 验收清单：新页面是否引入新元素类型/歧义场景/动态行为；是否登记在能力表；是否具备 data-testid/aria 元数据。

- 质量门禁
  - 构建/类型检查：Maven Wrapper（Windows: `mvnw.cmd`）。
  - 单测/集成/E2E：默认走 Mock Server；需要真实 LLM 时标记 ExternalServiceTest。
  - 回归基线：TTA 常见指令 90%+ 覆盖、交互元素 85%+ 覆盖、代码覆盖率 70%+。

- 里程碑（建议）
  1) M1：第一批工具暴露 + 测试通过 + 文档同步
  2) M2：高级交互（拖拽/鼠标移动/滚轮）与抓取 API 的选择性暴露
  3) M3：iframe/Shadow DOM 支持与策略测试
  4) M4：消歧策略与稳定性优化（stale 重试、元素置换、优先级）

- 需求对应
  - 目标正确转换：通过防御性校验 + 等待策略 + 工具协议保障（本计划“工具调用协议/测试计划”）。
  - 还需暴露操作与场景：见“优先/次优先”两批清单与对应场景。
  - 文档是否需更新：已更新工具清单与测试 README，并在本文件记录路线图。


## 后续可选增强建议（未实现，仅供参考）

### InteractiveElement

- 缓存上一步与当前提取结果做 diff，仅输出新增或消失的元素。
- 为每个元素增加一个简短 action hint（如 “可输入”, “可点击跳转”）。
- 针对长页面：按屏幕区域分 bucket（top/middle/bottom）再排序，减少偏向首屏元素。
- 将已操作过的元素在摘要中标记 (✔) ，避免模型重复点击。

### 新增测试

- LLM 的图像理解冒烟测试
