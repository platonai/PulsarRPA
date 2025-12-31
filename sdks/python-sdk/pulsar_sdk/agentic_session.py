from typing import Any, Dict, Optional

from .client import PulsarClient
from .models import (
    AgentActResult,
    AgentObservation,
    AgentRunResult,
    ExtractionResult,
    PageSnapshot,
    FieldsExtraction,
)
from .webdriver import WebDriver


class AgenticSession:
    """AgenticSession facade backed by the REST API."""

    def __init__(self, client: PulsarClient):
        self.client = client
        self._driver: Optional[WebDriver] = None
        self._process_trace = []

    @property
    def driver(self) -> WebDriver:
        if self._driver is None:
            self._driver = WebDriver(self.client)
        return self._driver

    # PulsarSession-like operations
    def normalize(self, url: str, args: Optional[str] = None) -> Dict[str, Any]:
        return self.client.post("/session/{sessionId}/normalize", {"url": url, "args": args})

    def open(self, url: str, args: Optional[str] = None) -> PageSnapshot:
        value = self.client.post("/session/{sessionId}/open", {"url": url, "args": args})
        return PageSnapshot(url=value.get("url", url), html=value.get("html"))

    def load(self, url: str, args: Optional[str] = None) -> PageSnapshot:
        value = self.client.post("/session/{sessionId}/load", {"url": url, "args": args})
        return PageSnapshot(url=value.get("url", url), html=value.get("html"))

    def submit(self, url: str, args: Optional[str] = None) -> Any:
        return self.client.post("/session/{sessionId}/submit", {"url": url, "args": args})

    def parse(self, page: PageSnapshot) -> Any:
        # Not exposed via REST; callers should parse locally; here we simply return HTML
        return page.html

    # Parsing / extraction helpers
    def extract(self, document: Any, fields: Dict[str, str]) -> FieldsExtraction:
        value = self.client.post(
            "/session/{sessionId}/agent/extract",
            {"instruction": "extract fields", "selectors": fields},
        )
        return FieldsExtraction(fields=value if isinstance(value, dict) else {"result": value})

    # Agentic operations
    def run(self, instruction: str) -> AgentRunResult:
        return self.agent_run(instruction)

    def agent_run(self, instruction: str) -> AgentRunResult:
        value = self.client.post(
            "/session/{sessionId}/agent/run", {"instruction": instruction}
        )
        trace = value.get("trace") if isinstance(value, dict) else None
        if trace:
            self._process_trace.extend(trace)
        return AgentRunResult(finalResult=value.get("finalResult") if isinstance(value, dict) else value, trace=trace)

    def act(self, action: str) -> AgentActResult:
        return self.agent_act(action)

    def agent_act(self, action: str) -> AgentActResult:
        value = self.client.post("/session/{sessionId}/agent/act", {"action": action})
        trace = value.get("trace") if isinstance(value, dict) else None
        if trace:
            self._process_trace.extend(trace)
        return AgentActResult(result=value.get("result") if isinstance(value, dict) else value, trace=trace)

    def observe(self, instruction: str) -> AgentObservation:
        return self.agent_observe(instruction)

    def agent_observe(self, instruction: str) -> AgentObservation:
        value = self.client.post(
            "/session/{sessionId}/agent/observe", {"instruction": instruction}
        )
        return AgentObservation(observations=value)

    def summarize(self, instruction: str, selector: Optional[str] = None) -> str:
        return self.agent_summarize(instruction, selector)

    def agent_summarize(self, instruction: str, selector: Optional[str] = None) -> str:
        value = self.client.post(
            "/session/{sessionId}/agent/summarize",
            {"instruction": instruction, "selector": selector},
        )
        return value.get("summary") if isinstance(value, dict) else value

    def clear_history(self) -> Any:
        return self.agent_clear_history()

    def agent_clear_history(self) -> Any:
        cleared = self.client.post("/session/{sessionId}/agent/clearHistory", {})
        self._process_trace.clear()
        return cleared

    # Convenience aliases mirroring Kotlin usage
    def get_or_create_bound_driver(self) -> WebDriver:
        return self.driver

    def capture(self, driver: Optional[WebDriver] = None) -> PageSnapshot:
        # Without a dedicated capture endpoint, re-open the current URL to refresh HTML
        drv = driver or self.driver
        current_url = drv.get_current_url()
        value = self.client.post("/session/{sessionId}/open", {"url": current_url})
        return PageSnapshot(url=value.get("url", current_url), html=value.get("html"))

    @property
    def companion_agent(self):
        return self

    @property
    def processTrace(self):  # Kotlin-style alias
        return list(self._process_trace)

    @property
    def process_trace(self):  # Pythonic
        return list(self._process_trace)

    @property
    def context(self):
        return self

    def close(self):
        self.client.delete_session()


__all__ = ["AgenticSession"]

