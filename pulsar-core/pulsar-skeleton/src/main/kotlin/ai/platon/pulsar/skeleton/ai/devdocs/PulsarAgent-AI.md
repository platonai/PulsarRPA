# 🚦 PerceptiveAgent Developer Guide

## 📋 Prerequisites

Before starting development, ensure you understand:

1. Root `README-AI.md` – Global development guidelines and project structure
2. Project architecture – Multi-module Maven project with Kotlin as primary language
3. Testing guidelines – Comprehensive testing strategy with unit, integration, and E2E tests
   - Unless explicitly required, web page access during testing must use the Mock Server
   - Relevant web page resources are located in `pulsar-tests-common/src/main/resources/static/generated/tta`

## 🎯 Overview

[PerceptiveAgent.kt](../PerceptiveAgent.kt) is an enterprise-grade multi-round planning executor that enables AI models to perform
web automation through screenshot observation and historical action analysis. It plans and executes atomic
actions step-by-step until the target is achieved.

### Key Architecture Principles

- Atomic actions: Each step performs exactly one atomic action (single click, single input, single selection)
- Multi-round planning: The model plans the next action based on screenshot + action history
- Structured output: The model returns JSON-formatted function calls
- Termination control: Loop ends via `taskComplete=true`
- Result summarization: Final summary generated using `operatorSummarySchema`
- Error resilience: Graceful handling with fallback strategies
- Safety first: URL validation and a secure execution environment

## 🧪 Testing Strategy

### Integration tests
- Real browser automation with Spring context

### Coverage areas
1. Action execution pipeline – All tool calls (navigation, interactions, scrolling, screenshots)

## 📌 Plan: TTA → WebDriver correctness (docs/API/tests kept in sync)

Goal: Ensure natural language is stably converted into executable WebDriver actions; align documentation, tool list, and implementation; and augment test coverage.

- Baseline status
  - TextToAction already exposes tools mapped to driver: navigateTo, waitForSelector, click, fill, press, check/uncheck, scrollDown/Up/Top/Bottom/Middle, scrollToScreen, clickTextMatches, clickMatches, clickNthAnchor, captureScreenshot (with optional selector), delay, stop.
  - MiniWebDriver includes many more capabilities not yet exposed as tool_call (e.g., exists/isVisible/focus/scrollTo(selector)/waitForNavigation).
  - Test pages are centralized under `pulsar-tests-common/.../static/generated/tta`; all TTA tests should rely solely on this Mock Server.

- High-priority new tools to expose (Batch 1 – improves robustness and observability)
  1) exists(selector), isVisible(selector)
  2) focus(selector)
  3) scrollTo(selector)
  4) waitForNavigation(oldUrl? = "", timeoutMillis? = policy)

- Next priorities (Batch 2 – richer interactions)
  - goBack(), goForward()
  - type(selector, text)
  - mouseWheelDown/Up(...), moveMouseTo(x,y)/moveMouseTo(selector,dx,dy), dragAndDrop(selector,dx,dy)
  - outerHTML()/outerHTML(selector)
  - selectText/Attributes/Property/Hyperlinks/Images family
  - getCookies(), url()/currentUrl()/documentURI()/baseURI()/referrer(), bringToFront()

- Tool-call protocol (prompt rules)
  - Return JSON only; do not guess selectors; when ambiguous or unsure, return an empty `{ "tool_calls": [] }`.
  - Prefer selectors from the interactive element list; before clicking, optionally use waitForSelector and exists/isVisible for defensive validation.
  - After navigation-type actions, suggest `waitForNavigation`.

- Element extraction and context
  - Current extraction covers input/textarea/select/button/a[href] and explicit interactivity (onclick/contenteditable/role=button|link), filters hidden/disabled.
  - Does not yet support iframe/Shadow DOM; defer until dedicated pages land, then extend extraction and tests.
  - Locator priority recommendation: data-testid > aria-label > role+name > text > relative position.

- Documentation alignment tasks
  - Update TOOL_CALL_LIST to include implemented-but-not-yet-exposed items (done: scrollToScreen, clickTextMatches, clickMatches, clickNthAnchor).
  - In `pulsar-tests/.../tta/README-AI.md`, note that selection/extraction APIs aren’t yet exposed as tool calls to avoid confusion (done).
  - Add this unified plan (this section) to keep effort synchronized.

- Test plan
  - For Batch 1 tools, add minimal reproducible cases (Mock pages + integration tests).
  - Add real-world scenarios like conditional actions and post-click navigation waits.
  - Expand Mock pages: dynamic/ambiguity, a11y metadata, long lists/multi-screen; later add iframe/Shadow DOM pages.
  - Acceptance checklist: new page introduces new element types/ambiguity/dynamic behavior; listed in capability table; includes data-testid/aria metadata.

- Quality gates
  - Build/typecheck via Maven Wrapper (Windows: `mvnw.cmd`).
  - Unit/integration/E2E default to Mock Server; tag ExternalServiceTest when real LLM is required.
  - Baselines: 90%+ for common commands, 85%+ for interactive elements, ≥70% code coverage.

- Milestones (suggested)
  1) M1: Batch 1 tools exposed + tests passing + docs updated
  2) M2: Advanced interactions (drag/mouse move/wheel) and selective extraction APIs
  3) M3: iframe/Shadow DOM support and strategy tests
  4) M4: Disambiguation strategies and stability (stale retries, element replacement, priorities)

- Requirements mapping
  - Correct conversion: defensive checks + waits + tool protocol ensure correctness.
  - Needed capabilities and scenarios: see the two batches and their examples.
  - Documentation updates: tool list and test README updated; this plan recorded here.

## Optional enhancements (not implemented; for reference)

### InteractiveElement

- Cache previous vs current extraction results and only output added/removed elements.
- Add short action hints per element (e.g., “typeable”, “click to navigate”).
- For long pages: bucket elements by screen regions (top/middle/bottom) to reduce above-the-fold bias.
- Mark elements already operated on in the summary (✓) to discourage repeat clicks.
