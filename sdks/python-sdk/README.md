# Pulsar Browser Python SDK (early draft)

Thin hand-written client over the Browser4 OpenAPI (`openapi/openapi.yaml`).

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

# Navigate
session.driver.navigate_to("https://example.com")
print(session.driver.get_current_url())

# Agentic actions
run_result = session.agent_run("scroll to the bottom of the page")
print(run_result.finalResult)

# Close session
session.close()
```

## Surface area (MVP)
- Session: `create_session`, `delete_session`
- PulsarSession-like: `open`, `load`, `submit`, `normalize`, `capture`, `extract`
- Agentic: `agent_run`, `agent_act`, `agent_observe`, `agent_summarize`, `agent_clear_history`, trace via `process_trace`
- WebDriver: navigation (`navigate_to`, `get_current_url`), selectors (`exists`, `wait_for`, `find_element_by_selector`, `find_elements_by_selector`, `click`, `fill`, `press`, `outer_html`, `screenshot`), element-id APIs (`find_element`, `find_elements`, `click_element`, `send_keys`, `get_attribute`, `get_text`), scripts (`execute_script`, `execute_async_script`), control (`delay`, `pause`, `stop`), events (`create_event_config`, `list_event_configs`, `get_events`, `subscribe_events`)

## Testing

```bash
pytest
```

## Notes
- `capture` currently re-opens the current URL (no dedicated REST endpoint yet).
- Minimal data models; extend with richer schemas as needed.
- Add auth headers/timeouts as required for your deployment.

