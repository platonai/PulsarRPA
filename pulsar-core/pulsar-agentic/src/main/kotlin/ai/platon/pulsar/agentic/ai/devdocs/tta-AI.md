# 🚦 Coder Guideline For TextToAction

## 🎯 Purpose

Design an AI agent for browser automation.
Its main job is to translate the user’s natural language command into a callable API inside MiniWebDriver.

---

## 📌 Main Responsibilities

1. Action Translation

    - Implement and wire logic in `ai.platon.pulsar.agentic.ai.tta.TextToAction#generate`
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

    - Use the elements snapshot (ranked interactive elements), not full DOM
    - Use the strict JSON response formats defined below (no markdown, no extra text)

4. Interactive Element Handling

    - The element list is computed by running JavaScript on the active DOM
    - Ensure element references are stable against DOM mutations

5. Fallback Rule

    - If no suitable interactive element exists for the requested action, or the task cannot proceed in this step, return the Completion JSON (see below) with a concise summary and a few suggestions

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
- Output (EXACT ONE action or a completion)
  - Prefer an action for the next atomic step; if not possible, return a completion object

Success criteria
- Returns exactly one actionable driver line (or a completion) that compiles against MiniWebDriver
- Selector is stable and present in the provided element list when required
- No hallucinated selectors or parameters

---

## MiniWebDriver API (subset for TTA)

Allowed calls → driver methods mapping:
- click(selector: String) → `driver.click(selector)`
- type(selector: String, text: String) → `driver.type(selector, text)`
- navigateTo(url: String) → `driver.navigateTo(url)`
- scrollDown(count: Int = 1) → `driver.scrollDown(count)`
- scrollUp(count: Int = 1) → `driver.scrollUp(count)`
- scrollToMiddle(ratio: Double = 0.5) → `driver.scrollToMiddle(ratio)`
- waitForNavigation(oldUrl: String = current, timeoutMillis: Long = 3000) → `driver.waitForNavigation(oldUrl, timeoutMillis)`

Note: The agent must produce exactly one driver step per generation. For multi-step user asks, choose the highest-value primary step first (see tie-break rules below).

---

## Response Schema (JSON only)

The model must return JSON only, no code fences or extra text. There are two supported formats:

1) Action (preferred when a next atomic step is clear)

{
  "elements": [
    {
      "locator": string,            // a stable selector for the target element (optional for no-selector actions)
      "description": string,        // short human-friendly description
      "method": string,             // one of the allowed MiniWebDriver methods (e.g., click, type, scrollToMiddle)
      "arguments": [                // ordered arguments besides locator (if any)
        { "name": string, "value": string }
      ]
    }
  ]
}

- Only one element is expected per step.
- For actions that don’t require a selector (e.g., scrollToMiddle), locator can be omitted.

2) Completion (when task is done or cannot proceed this step)

{
  "isComplete": true,
  "summary": string,
  "suggestions": [string]
}

Rules
1) JSON only; no comments/markdown/code fences
2) Prefer an action when safe and obvious; otherwise return the completion JSON
3) Never fabricate selectors or arguments not present in the element list/context
4) Emit at most one actionable element in the "elements" array

---

## Element Selection Heuristics (must)

Align with `docs/development/dom/interactive-elements.md`:

- Candidate pool: input, textarea, select, button, a[href], or elements with explicit interactivity (onclick, contenteditable=true, role=button/link)
- Visibility: must be visible, not disabled, not type=hidden
- Stability: prefer selectors built from
  1) id → `#id`
  2) data-testid → `[data-testid=...]`
  3) name → `tag[name=...]`
  4) onclick attribute → `tag[onclick=...]`
  5) a[href=...] for links
  6) tag[type=...] fallback; otherwise nth-of-type path
- Semantic fit
  - type → prefer input/textarea with matching placeholder/text/type
  - click → prefer button/link whose text matches the intent (e.g., next, search, submit) or has relevant role/onclick
- Disambiguation
  - Prefer visible, on-screen elements closer to view center/top
  - If multiple good matches, choose the one with the strongest selector (id > data-testid > name > href > type > path)

Reject if
- selector is empty or not in provided element list
- element is not visible or disabled

---

## Action Selection Rules

- EXACT ONE call per step
- Primary intent mapping examples
  - "scroll to the middle" → `scrollToMiddle()` (no selector needed)
  - "scroll down a bit" → `scrollDown(1)` (or parsed count)
  - "go back" → not in the subset above; return Completion JSON
  - "search `best AI toy`" (single-step ask) → choose the highest-value primary step:
    - If a clear search box exists → `type(<search>, "best AI toy")`
    - If no input but a clear search button exists → `click(<search-button>)`
  - "type X in the search box" → `type(<search-input>, "X")`
  - "click next" → `click(<next-button-or-link>)`

Tie-breaker policy
- If the user asks for multiple steps in one sentence, pick the most specific interaction that directly advances the task; do not emit two tool calls in this single-action mode

---

## Fallback Behavior (must)

- If no suitable interactive element exists for the action, or the action is unsafe/ambiguous, return the Completion JSON with a short summary and 2-3 suggestions for next steps
- If the instruction requires a capability outside the allowed tool set (e.g., "go back"), return the Completion JSON

---

## Interactive Elements: Schema and Source

- Spec: `docs/development/dom/interactive-elements.md`
- The extraction runs JavaScript on the live DOM, returning a compact list of interactive candidates
- Only the element list should be surfaced to the model

---

## Examples

User → "scroll to the middle"

Action JSON →
{
  "elements": [
    {
      "method": "scrollToMiddle",
      "description": "Scroll viewport to middle",
      "arguments": [ { "name": "ratio", "value": "0.5" } ]
    }
  ]
}

User → "type best AI toy in the search box"

Elements (excerpt) →
- [input type='search'] ph='Search...' selector='#search-input'
- [button type='submit'] 'Search' selector='#submit-btn'

Action JSON →
{
  "elements": [
    {
      "locator": "#search-input",
      "description": "Search input",
      "method": "type",
      "arguments": [ { "name": "text", "value": "best AI toy" } ]
    }
  ]
}

User → "click next"

Elements (excerpt) →
- [a] 'Next' selector='a[href="/page/2"]'
- [button] 'Continue' selector='#continue'

Action JSON →
{
  "elements": [
    {
      "locator": "a[href=\"/page/2\"]",
      "description": "Next link",
      "method": "click",
      "arguments": []
    }
  ]
}

User → ambiguous or no element

Completion JSON →
{
  "isComplete": true,
  "summary": "Cannot find a reliable next action from current context.",
  "suggestions": [
    "Refine the query keywords",
    "Scroll or navigate to a results page",
    "Provide a specific button or link text"
  ]
}

---

## Implementation Notes

- Use `WebDriver` as the runtime interface (the MiniWebDriver contract maps to `ToolCallExecutor` methods)
- Reference spec: `docs/development/dom/interactive-elements.md`
- See `TextToAction.kt` for prompt assembly and response parsing; match its schema and limits
- Tests to learn from will be updated; temporarily ignore legacy tool_calls tests

---

## Quick Quality Gates

- Build: project root with Maven wrapper
- Lint/Typecheck: Kotlin/Java compile should pass
- Unit tests: add or run tests under `pulsar-tests`

On Windows cmd.exe from project root:

```
:: build all
mvnw.cmd -q -DskipTests package

:: run a focused test suite (example)
mvnw.cmd -q -pl pulsar-tests -Dtest="ai.platon.pulsar.skeleton.ai.tta.TextToActionBasicTest" test
```

---

## Next Steps (optional improvements)

- Add scoring for element ranking (text/placeholder/type/position)
- Add multilingual intent keywords (搜索/search, 提交/submit, 上一页/previous, 下一页/next)
- Expand tool set (e.g., goBack(), reload(), selectOption()) and update mapping and tests accordingly
  - doubleClick
  - dragAndDrop
  - selectOptions
