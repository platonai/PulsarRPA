# Pulsar SDK Package

This package provides the core Browser4 Python SDK implementation.

## Modules

### `client.py`
Low-level HTTP client for Browser4 API communication.

### `models.py`
Data models matching Kotlin interfaces:
- `WebPage` - Loaded page result
- `NormURL` - Normalized URL with arguments
- `AgentRunResult` - Agent run operation result
- `AgentActResult` - Agent action result
- `AgentObservation` / `ObserveResult` - Page observation results
- `ExtractionResult` - AI extraction result
- `PageSnapshot` - Captured page state
- `FieldsExtraction` - CSS selector extraction result
- `PageEventHandlers` - Event handlers placeholder

### `webdriver.py`
WebDriver-compatible browser control:
- Navigation (navigate_to, go_back, go_forward, reload)
- Element interaction (click, fill, type, press, hover, focus)
- Scrolling (scroll_down, scroll_up, scroll_to, scroll_to_bottom, scroll_to_top)
- Selection (exists, wait_for_selector, select_first_text, select_text_all)
- Screenshots (capture_screenshot)
- Script execution (execute_script, evaluate)

### `agentic_session.py`
Session management with AI-powered automation:
- `PulsarSession` - Page loading and extraction (open, load, submit, normalize, extract, scrape)
- `AgenticSession` - AI-powered actions (act, run, observe, summarize, agent_extract)

## Usage

```python
from pulsar_sdk import PulsarClient, AgenticSession

client = PulsarClient(base_url="http://localhost:8182")
client.create_session()
session = AgenticSession(client)

# Navigation and extraction
page = session.open("https://example.com")
fields = session.extract(page, {"title": "h1"})

# AI-powered actions
result = session.act("click the search button")
history = session.run("search for 'python'")

session.close()
```

See the parent README.md for comprehensive documentation.

