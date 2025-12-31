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
print(run_result)

# Clean up
session.close()
```

## Scope
- Mirrors key Kotlin surfaces used in `FusedActs.kt`: `open/load/submit`, `extract`, `agent.run/act/observe/summarize/clearHistory`, WebDriver selector ops.
- Backed by REST endpoints in `openapi/openapi.yaml`.

## Notes
- `capture` currently re-opens the current URL (no dedicated REST endpoint yet).
- Minimal data models; extend with richer schemas as needed.
- Add auth headers/timeouts as required for your deployment.

