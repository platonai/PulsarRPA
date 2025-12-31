from typing import Any, Dict, List, Optional

from .client import PulsarClient


class WebDriver:
    """WebDriver-compatible façade mapping to selector-first REST endpoints."""

    def __init__(self, client: PulsarClient):
        self.client = client

    # Navigation
    def navigate_to(self, url: str) -> Any:
        return self.client.post("/session/{sessionId}/url", {"url": url})

    def get_current_url(self) -> str:
        return self.client.get("/session/{sessionId}/url")

    def get_document_uri(self) -> str:
        return self.client.get("/session/{sessionId}/documentUri")

    def get_base_uri(self) -> str:
        return self.client.get("/session/{sessionId}/baseUri")

    # Selector-first helpers
    def exists(self, selector: str, strategy: str = "css") -> bool:
        value = self.client.post("/session/{sessionId}/selectors/exists", {"selector": selector, "strategy": strategy})
        if isinstance(value, dict):
            return bool(value.get("exists"))
        return bool(value)

    def wait_for(self, selector: str, strategy: str = "css", timeout: int = 30000) -> bool:
        value = self.client.post(
            "/session/{sessionId}/selectors/waitFor",
            {"selector": selector, "strategy": strategy, "timeout": timeout},
        )
        if value is None:
            return True
        return bool(value.get("exists")) if isinstance(value, dict) else bool(value)

    def find_element_by_selector(self, selector: str, strategy: str = "css") -> Dict[str, Any]:
        return self.client.post("/session/{sessionId}/selectors/element", {"selector": selector, "strategy": strategy})

    def find_elements_by_selector(self, selector: str, strategy: str = "css") -> List[Dict[str, Any]]:
        return self.client.post("/session/{sessionId}/selectors/elements", {"selector": selector, "strategy": strategy})

    def click(self, selector: str, strategy: str = "css") -> Any:
        return self.client.post("/session/{sessionId}/selectors/click", {"selector": selector, "strategy": strategy})

    def fill(self, selector: str, text: str, strategy: str = "css") -> Any:
        return self.client.post(
            "/session/{sessionId}/selectors/fill",
            {"selector": selector, "strategy": strategy, "value": text},
        )

    def press(self, selector: str, key: str, strategy: str = "css") -> Any:
        return self.client.post(
            "/session/{sessionId}/selectors/press",
            {"selector": selector, "strategy": strategy, "key": key},
        )

    def outer_html(self, selector: str, strategy: str = "css") -> str:
        value = self.client.post(
            "/session/{sessionId}/selectors/outerHtml",
            {"selector": selector, "strategy": strategy},
        )
        return value.get("outerHtml") if isinstance(value, dict) else value

    def screenshot(self, selector: Optional[str] = None, strategy: str = "css") -> bytes:
        payload = {"strategy": strategy}
        if selector:
            payload["selector"] = selector
        return self.client.post("/session/{sessionId}/selectors/screenshot", payload)

    # Element-by-id APIs
    def find_element(self, using: str, value: str) -> Dict[str, Any]:
        return self.client.post("/session/{sessionId}/element", {"using": using, "value": value})

    def find_elements(self, using: str, value: str) -> List[Dict[str, Any]]:
        return self.client.post("/session/{sessionId}/elements", {"using": using, "value": value})

    def click_element(self, element_id: str) -> Any:
        return self.client.post(f"/session/{{sessionId}}/element/{element_id}/click", {})

    def send_keys(self, element_id: str, text: str) -> Any:
        return self.client.post(f"/session/{{sessionId}}/element/{element_id}/value", {"text": [text]})

    def get_attribute(self, element_id: str, name: str) -> Any:
        return self.client.get(f"/session/{{sessionId}}/element/{element_id}/attribute/{name}")

    def get_text(self, element_id: str) -> str:
        return self.client.get(f"/session/{{sessionId}}/element/{element_id}/text")

    # Script execution
    def execute_script(self, script: str, args: Optional[list] = None) -> Any:
        return self.client.post(
            "/session/{sessionId}/execute/sync",
            {"script": script, "args": args or []},
        )

    def execute_async_script(self, script: str, args: Optional[list] = None, timeout: int = 30000) -> Any:
        return self.client.post(
            "/session/{sessionId}/execute/async",
            {"script": script, "args": args or [], "timeout": timeout},
        )

    # Control
    def delay(self, millis: int) -> Any:
        return self.client.post("/session/{sessionId}/control/delay", {"duration": millis})

    def pause(self) -> Any:
        return self.client.post("/session/{sessionId}/control/pause", {})

    def stop(self) -> Any:
        return self.client.post("/session/{sessionId}/control/stop", {})

    # Events
    def create_event_config(self, config: Dict[str, Any]) -> Any:
        return self.client.post("/session/{sessionId}/event-configs", config)

    def list_event_configs(self) -> Any:
        return self.client.get("/session/{sessionId}/event-configs")

    def get_events(self) -> Any:
        return self.client.get("/session/{sessionId}/events")

    def subscribe_events(self, subscribe_request: Dict[str, Any]) -> Any:
        return self.client.post("/session/{sessionId}/events/subscribe", subscribe_request)


__all__ = ["WebDriver"]

