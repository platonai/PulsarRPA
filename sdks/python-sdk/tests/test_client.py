import json
from typing import Any
from pathlib import Path
import sys

# Ensure the local package is importable without installation
ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

import pytest

from pulsar_sdk import PulsarClient, AgenticSession


class StubResponse:
    def __init__(self, status_code: int = 200, payload: Any = None):
        self.status_code = status_code
        self._payload = payload
        self.content = json.dumps(payload).encode("utf-8") if payload is not None else b""

    def raise_for_status(self):
        if 400 <= self.status_code:
            raise Exception(f"HTTP {self.status_code}")

    def json(self):
        return self._payload


class StubRequestsSession:
    def __init__(self):
        self.calls = []

    def request(self, method, url, headers=None, data=None, timeout=None):
        self.calls.append({"method": method, "url": url, "headers": headers, "data": data, "timeout": timeout})
        # Echo back wrapped in { value: ... } as WebDriver usually does
        body = json.loads(data) if data else None
        if url.endswith("/session") and method == "POST":
            return StubResponse(payload={"value": {"sessionId": "abc123"}})
        if url.endswith("/url") and method == "POST":
            return StubResponse(payload={"value": body})
        if url.endswith("/agent/run"):
            return StubResponse(payload={"value": {"finalResult": "done", "trace": ["t1"]}})
        return StubResponse(payload={"value": body or {}})

    def close(self):
        pass


@pytest.fixture()
def stub_client(monkeypatch):
    client = PulsarClient(base_url="http://localhost:8182")
    stub = StubRequestsSession()
    monkeypatch.setattr(client, "session", stub)
    return client, stub


def test_create_session_sets_id(stub_client):
    client, stub = stub_client
    session_id = client.create_session()
    assert session_id == "abc123"
    assert client.session_id == "abc123"
    assert stub.calls[-1]["url"].endswith("/session")


def test_agentic_session_run_and_trace(stub_client):
    client, stub = stub_client
    client.session_id = "abc123"
    session = AgenticSession(client)
    result = session.agent_run("do it")
    assert result.finalResult == "done"
    assert result.trace == ["t1"]
    assert session.process_trace == ["t1"]


def test_driver_navigate_and_get_url(stub_client):
    client, stub = stub_client
    client.session_id = "abc123"
    session = AgenticSession(client)
    session.driver.navigate_to("https://example.com")
    assert stub.calls[-1]["url"].endswith("/session/abc123/url")
    assert json.loads(stub.calls[-1]["data"]) == {"url": "https://example.com"}
