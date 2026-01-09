# Pulsar Browser Python SDK (draft)

Thin hand-written client over the Browser4 OpenAPI (`openapi/openapi.yaml`). Mirrors key Kotlin surfaces (`WebDriver`, `AgenticSession`, `FusedActs`).

## Install (editable)

```bash
pip install -e .[dev]
```

## Quick start

```python
from pulsar_sdk import PulsarClient, AgenticSession

client = PulsarClient(base_url="http://localhost:8182")
session_id = client.create_session()
session = AgenticSession(client)

# WebDriver-like navigation
session.driver.navigate_to("https://example.com")
print(session.driver.get_current_url())

# Agentic actions (observe/act/run)
run_result = session.run("scroll to the bottom of the page")
print(run_result.finalResult)

# Extract fields via agent/extract
page = session.capture()
fields = session.extract(page.html, {"title": "title"})
print(fields.fields)

session.close()
```

## API coverage
- WebDriver: navigation (url/baseUri/documentUri), selector-first exists/wait/click/fill/press/outerHtml/screenshot, element find(s) + click/send_keys/get_attribute/get_text, execute sync/async script, control (delay/pause/stop), events (configs/list/get/subscribe).
- AgenticSession: normalize/open/load/submit, parse (local), extract (agent/extract), agent run/act/observe/summarize/clearHistory, capture fallback via open(current_url), driver getter and Kotlin-style aliases.
- Models: lightweight dataclasses for agent results, page snapshots, field extraction.

## Notes / gaps
- No dedicated `capture` or local DOM parse endpoint in OpenAPI; `capture` re-opens the current URL to refresh HTML.
- Auth headers can be injected via `PulsarClient(default_headers=...)`.
- Extend models if your deployment returns richer trace/span metadata.

