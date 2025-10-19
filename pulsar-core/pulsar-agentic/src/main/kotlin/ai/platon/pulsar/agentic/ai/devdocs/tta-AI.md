# 🚦 Coder Guideline For TextToAction

## 🎯 Purpose

Design an AI agent for browser automation.
Its main job is to translate the user’s natural language command into a callable API inside MiniWebDriver.

---

## 📌 Main Responsibilities

1. Action Translation

    - Implement logic in `ai.platon.pulsar.skeleton.ai.tta.TextToAction#generateWebDriverAction`
    - Generate EXACT ONE WebDriver action with interactive elements
    - Convert user’s plain text commands (e.g., "scroll to the middle") into concrete MiniWebDriver APIs

2. Element Selection

    - Always return the best matching interactive element for the required action
    - Example commands:
      - "type `best AI toy` in the search box"
      - "scroll to the middle"
      - "click next button"
      - "go back"
      - "search `best AI toy`"

3. Prompt Construction

    - Use tool-call style prompt and response
    - Provide interactive element lists only (not the entire page DOM)

4. Interactive Element Handling

    - The element list is computed by running JavaScript on the active DOM
    - Ensure element references are stable against DOM mutations

5. Fallback Rule

    - If no suitable interactive element exists for the requested action: generate an empty suspend function instead of throwing errors

---

## ✅ Key Design Goals

- Minimal, accurate action-to-API mapping
- Reliable element reference under DOM changes
- Efficient prompts leveraging only element list (avoid full DOM dumps)

---

## Contract: Inputs and Outputs

- Input
  - user command: natural language string
  - interactive elements: a short, ranked list built from the active DOM
- Output (EXACT ONE action)
  - A single WebDriver action rendered from an allowed API call, using a valid selector when needed
  - If uncertain or no element fits, output an empty action (no-op function)

Success criteria
- Returns exactly one actionable driver line (or a no-op) that compiles against MiniWebDriver
- Selector is stable and present in the provided element list
- No hallucinated selectors or parameters

---

## MiniWebDriver API (subset for TTA)

Allowed tool calls → driver methods mapping:
- click(selector: String) → `driver.click(selector)`
- fill(selector: String, text: String) → `driver.fill(selector, text)`
- navigateTo(url: String) → `driver.navigateTo(url)`
- scrollDown(count: Int = 1) → `driver.scrollDown(count)`
- scrollUp(count: Int = 1) → `driver.scrollUp(count)`
- scrollToMiddle(ratio: Double = 0.5) → `driver.scrollToMiddle(ratio)`
- waitForSelector(selector: String, timeoutMillis: Long = 5000) → `driver.waitForSelector(selector, timeoutMillis)`
- check(selector: String) → `driver.check(selector)`
- uncheck(selector: String) → `driver.uncheck(selector)`

Note: `generateWebDriverAction` must produce exactly one driver line. For multi-step user asks, choose the highest-value primary step first (see tie-break rules below).

---

## Tool-call Prompt and Response Schema

Use a strict JSON-only tool-call style. The model must return JSON only.

- Tools list (names and args) matches the MiniWebDriver subset above
- Response format (no extra text/markdown):

{
  "tool_calls": [
    {"name": "click", "args": {"selector": "#submit-btn"}}
  ]
}

Rules
1) JSON only; no comments/markdown/code fences
2) If unsure or no element, return {"tool_calls": []}
3) Never fabricate selectors or arguments
4) Max 1 tool call for `generateWebDriverAction`; for batched flows a higher limit may be used

The prompt should include only the interactive element descriptions, not raw DOM. See `buildToolUsePrompt` in `TextToAction.kt`.

---

## Element Selection Heuristics (must)

Align with `docs/development/dom/interactive-elements.md` and `EXTRACT_INTERACTIVE_ELEMENTS_EXPRESSION`:

- Candidate pool: input, textarea, select, button, a[href], or elements with explicit interactivity (onclick, contenteditable=true, role=button/link)
- Visibility: must be visible, not disabled, not hidden type
- Stability: prefer selectors built from
  1) id → `#id`
  2) data-testid → `[data-testid=...]`
  3) name → `tag[name=...]`
  4) onclick attribute → `tag[onclick=...]`
  5) a[href=...] for links
  6) tag[type=...] fallback; otherwise nth-of-type path
- Semantic fit
  - fill → prefer input/textarea with matching placeholder/text/type
  - click → prefer button/link whose text matches the intent (e.g., next, search, submit) or has relevant role/onclick
- Disambiguation
  - Prefer visible, on-screen elements closer to view center/top
  - If multiple good matches, choose the one with the strongest selector (id > data-testid > name > href > type > path)

Reject if
- selector is empty or not in provided element list
- element is not visible or disabled

---

## Action Selection Rules

- EXACT ONE call for `generateWebDriverAction`
- Primary intent mapping examples
  - "scroll to the middle" → `scrollToMiddle()` (no selector needed)
  - "scroll down a bit" → `scrollDown(1)` (or parsed count)
  - "go back" → Not in current subset → return empty function (no-op)
  - "search `best AI toy`" (single-step ask) → choose either the most probable primary step:
    - If a clear search box exists → `fill(<search>, "best AI toy")`
    - If no input but a clear search button exists → `click(<search-button>)`
  - "type X in the search box" → `fill(<search-input>, "X")`
  - "click next" → `click(<next-button-or-link>)`

Tie-breaker policy
- If the user asks for multiple steps in one sentence, pick the most specific interaction that directly advances the task; do not emit two tool calls in this single-action mode

---

## Fallback Behavior (must)

- If no suitable interactive element exists for the action, return an empty suspend function (no error)
- If the instruction requires a capability outside the allowed tool set (e.g., "go back" not supported here), return a no-op

---

## Interactive Elements: Schema and Source

- Spec: `docs/development/dom/interactive-elements.md`
- The extraction runs JavaScript on the live DOM, returning a compact list of interactive candidates
- Only the element list should be surfaced to the model

---

## Examples

User → "scroll to the middle"
Return →
{
  "tool_calls": [
    {"name": "scrollToMiddle", "args": {"ratio": 0.5}}
  ]
}

User → "type best AI toy in the search box"
Elements (excerpt) →
- [input type='search'] ph='Search...' selector='#search-input'
- [button type='submit'] 'Search' selector='#submit-btn'
Return →
{
  "tool_calls": [
    {"name": "fill", "args": {"selector": "#search-input", "text": "best AI toy"}}
  ]
}

User → "click next"
Elements (excerpt) →
- [a] 'Next' selector='a[href="/page/2"]'
- [button] 'Continue' selector='#continue'
Return →
{
  "tool_calls": [
    {"name": "click", "args": {"selector": "a[href=\"/page/2\"]"}}
  ]
}

User → ambiguous or no element
Return → {"tool_calls": []}

---

## Implementation Notes

- Use MiniWebDriver as the contract; when you code, use `WebDriver` as the interface
- Reference spec: `docs/development/dom/interactive-elements.md`
- Tests to learn from: `ai.platon.pulsar.skeleton.ai.tta.InteractiveElementExtractionTests`
- The prompt builder and JSON parser are in `TextToAction.kt`. Match its schema and limits.

---

## Quick Quality Gates

- Build: project root with Maven wrapper
- Lint/Typecheck: Kotlin/Java compile should pass
- Unit tests: add or run tests under `pulsar-tests`

On Windows cmd.exe from project root:

:: build all
mvnw.cmd -q -DskipTests package

:: run a focused test suite (example)
mvnw.cmd -q -pl pulsar-tests -Dtest="ai.platon.pulsar.skeleton.ai.tta.TextToActionBasicTest" test

---

## Next Steps (optional improvements)

- Add scoring for element ranking (text/placeholder/type/position)
- Add multilingual intent keywords (搜索/search, 提交/submit, 上一页/previous, 下一页/next)
- Expand tool set (e.g., back(), reload(), selectOption()) and update mapping and tests accordingly
  - double click
  - drag and drop
  - select options
