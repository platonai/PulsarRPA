"""
Unit tests for the Browser4 Python SDK.

These tests use stub responses to verify the SDK behavior without
requiring a running Browser4 server.
"""
import json
from typing import Any
from pathlib import Path
import sys

# Ensure the local package is importable without installation
ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

import pytest

from pulsar_sdk import (
    PulsarClient,
    PulsarSession,
    AgenticSession,
    WebDriver,
    WebPage,
    NormURL,
    AgentRunResult,
    AgentActResult,
    PageEventHandlers,
)


class StubResponse:
    """Mock HTTP response for testing."""
    
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
    """Mock requests session for testing."""
    
    def __init__(self):
        self.calls = []

    def request(self, method, url, headers=None, data=None, timeout=None):
        self.calls.append({
            "method": method,
            "url": url,
            "headers": headers,
            "data": data,
            "timeout": timeout
        })
        body = json.loads(data) if data else None
        
        # Route responses based on endpoint
        if url.endswith("/session") and method == "POST":
            return StubResponse(payload={"value": {"sessionId": "test-session-123"}})
        
        if "/url" in url and method == "POST":
            return StubResponse(payload={"value": body})
        
        if "/url" in url and method == "GET":
            return StubResponse(payload={"value": "https://example.com"})
        
        if "/normalize" in url:
            return StubResponse(payload={"value": {
                "spec": body.get("url", ""),
                "url": body.get("url", ""),
                "args": body.get("args"),
                "isNil": False
            }})
        
        if "/open" in url:
            return StubResponse(payload={"value": {
                "url": body.get("url", ""),
                "location": body.get("url", ""),
                "contentType": "text/html",
                "contentLength": 1024,
                "protocolStatus": "200 OK",
                "isNil": False
            }})
        
        if "/load" in url:
            return StubResponse(payload={"value": {
                "url": body.get("url", ""),
                "location": body.get("url", ""),
                "contentType": "text/html",
                "contentLength": 2048,
                "protocolStatus": "200 OK (cached)",
                "isNil": False
            }})
        
        if "/submit" in url:
            return StubResponse(payload={"value": True})
        
        if "/agent/run" in url:
            return StubResponse(payload={"value": {
                "success": True,
                "message": "Task completed",
                "historySize": 5,
                "processTraceSize": 3,
                "finalResult": "done",
                "trace": ["step1", "step2"]
            }})
        
        if "/agent/act" in url:
            return StubResponse(payload={"value": {
                "success": True,
                "message": "Action executed",
                "action": body.get("action", ""),
                "isComplete": True,
                "trace": ["action_trace"]
            }})
        
        if "/agent/observe" in url:
            return StubResponse(payload={"value": [{
                "locator": "0,123",
                "method": "click",
                "description": "Click button"
            }]})
        
        if "/agent/summarize" in url:
            return StubResponse(payload={"value": "Page summary text"})
        
        if "/agent/clearHistory" in url:
            return StubResponse(payload={"value": True})
        
        if "/agent/extract" in url:
            return StubResponse(payload={"value": {
                "success": True,
                "message": "Extracted",
                "data": {"field1": "value1"}
            }})
        
        if "/selectors/exists" in url:
            return StubResponse(payload={"value": {"exists": True}})
        
        if "/selectors/waitFor" in url:
            return StubResponse(payload={"value": {"exists": True}})
        
        if "/execute/sync" in url:
            return StubResponse(payload={"value": "script result"})
        
        if "/control/delay" in url:
            return StubResponse(payload={"value": None})
        
        # Default response
        return StubResponse(payload={"value": body or {}})

    def close(self):
        pass


@pytest.fixture()
def stub_client(monkeypatch):
    """Create a PulsarClient with stubbed HTTP session."""
    client = PulsarClient(base_url="http://localhost:8182")
    stub = StubRequestsSession()
    monkeypatch.setattr(client, "session", stub)
    return client, stub


# ========== PulsarClient Tests ==========

def test_create_session_sets_id(stub_client):
    """Test that create_session returns and stores session ID."""
    client, stub = stub_client
    session_id = client.create_session()
    
    assert session_id == "test-session-123"
    assert client.session_id == "test-session-123"
    assert stub.calls[-1]["url"].endswith("/session")


def test_create_session_with_capabilities(stub_client):
    """Test create_session with custom capabilities."""
    client, stub = stub_client
    session_id = client.create_session(capabilities={"browserName": "chrome"})
    
    assert session_id == "test-session-123"
    request_body = json.loads(stub.calls[-1]["data"])
    assert request_body["capabilities"] == {"browserName": "chrome"}


# ========== PulsarSession Tests ==========

def test_pulsar_session_properties(stub_client):
    """Test PulsarSession property accessors."""
    client, _ = stub_client
    client.session_id = "test-session-123"
    session = PulsarSession(client)
    
    assert session.uuid == "test-session-123"
    assert session.is_active
    assert "PulsarSession" in session.display


def test_pulsar_session_normalize(stub_client):
    """Test URL normalization."""
    client, stub = stub_client
    client.session_id = "test-session-123"
    session = PulsarSession(client)
    
    result = session.normalize("https://example.com", args="-expire 1d")
    
    assert isinstance(result, NormURL)
    assert result.url == "https://example.com"
    assert not result.is_nil


def test_pulsar_session_open(stub_client):
    """Test opening a URL immediately."""
    client, stub = stub_client
    client.session_id = "test-session-123"
    session = PulsarSession(client)
    
    page = session.open("https://example.com")
    
    assert isinstance(page, WebPage)
    assert page.url == "https://example.com"
    assert page.content_type == "text/html"
    assert not page.is_nil


def test_pulsar_session_load(stub_client):
    """Test loading a URL (from cache or internet)."""
    client, stub = stub_client
    client.session_id = "test-session-123"
    session = PulsarSession(client)
    
    page = session.load("https://example.com", args="-expire 1d")
    
    assert isinstance(page, WebPage)
    assert page.url == "https://example.com"
    
    # Verify args were sent
    request_body = json.loads(stub.calls[-1]["data"])
    assert request_body["args"] == "-expire 1d"


def test_pulsar_session_submit(stub_client):
    """Test submitting a URL to crawl pool."""
    client, stub = stub_client
    client.session_id = "test-session-123"
    session = PulsarSession(client)
    
    result = session.submit("https://example.com")
    
    assert result is True


def test_pulsar_session_driver(stub_client):
    """Test getting the bound WebDriver."""
    client, _ = stub_client
    client.session_id = "test-session-123"
    session = PulsarSession(client)
    
    driver = session.driver
    assert isinstance(driver, WebDriver)
    
    # Same instance should be returned
    assert session.driver is driver


# ========== AgenticSession Tests ==========

def test_agentic_session_run(stub_client):
    """Test running an autonomous task."""
    client, stub = stub_client
    client.session_id = "test-session-123"
    session = AgenticSession(client)
    
    result = session.run("complete the task")
    
    assert isinstance(result, AgentRunResult)
    assert result.success
    assert result.message == "Task completed"
    assert result.history_size == 5


def test_agentic_session_run_and_trace(stub_client):
    """Test that run operations update process trace."""
    client, stub = stub_client
    client.session_id = "test-session-123"
    session = AgenticSession(client)
    
    result = session.agent_run("do it")
    
    assert result.final_result == "done"
    assert result.trace == ["step1", "step2"]
    assert session.process_trace == ["step1", "step2"]


def test_agentic_session_act(stub_client):
    """Test executing a single action."""
    client, stub = stub_client
    client.session_id = "test-session-123"
    session = AgenticSession(client)
    
    result = session.act("click the button")
    
    assert isinstance(result, AgentActResult)
    assert result.success
    assert result.is_complete


def test_agentic_session_observe(stub_client):
    """Test observing the page."""
    client, stub = stub_client
    client.session_id = "test-session-123"
    session = AgenticSession(client)
    
    observations = session.observe("what can I do?")
    
    assert len(observations.observations) > 0
    assert observations.observations[0].method == "click"


def test_agentic_session_summarize(stub_client):
    """Test page summarization."""
    client, stub = stub_client
    client.session_id = "test-session-123"
    session = AgenticSession(client)
    
    summary = session.summarize("Summarize this page")
    
    assert summary == "Page summary text"


def test_agentic_session_clear_history(stub_client):
    """Test clearing agent history."""
    client, stub = stub_client
    client.session_id = "test-session-123"
    session = AgenticSession(client)
    
    # Add some trace
    session._process_trace = ["item1", "item2"]
    
    result = session.clear_history()
    
    assert result is True
    assert session.process_trace == []


def test_agentic_session_companion_agent(stub_client):
    """Test companion_agent returns self."""
    client, _ = stub_client
    client.session_id = "test-session-123"
    session = AgenticSession(client)
    
    assert session.companion_agent is session


def test_agentic_session_agent_extract(stub_client):
    """Test AI-powered extraction."""
    client, stub = stub_client
    client.session_id = "test-session-123"
    session = AgenticSession(client)
    
    result = session.agent_extract(
        instruction="Extract product names",
        schema={"type": "array"}
    )
    
    assert result.success
    assert result.data == {"field1": "value1"}


# ========== WebDriver Tests ==========

def test_webdriver_navigate(stub_client):
    """Test WebDriver navigation."""
    client, stub = stub_client
    client.session_id = "test-session-123"
    driver = WebDriver(client)
    
    driver.navigate_to("https://example.com")
    
    assert stub.calls[-1]["url"].endswith("/session/test-session-123/url")
    assert json.loads(stub.calls[-1]["data"]) == {"url": "https://example.com"}


def test_webdriver_current_url(stub_client):
    """Test getting current URL."""
    client, stub = stub_client
    client.session_id = "test-session-123"
    driver = WebDriver(client)
    
    url = driver.current_url()
    
    assert url == "https://example.com"


def test_webdriver_exists(stub_client):
    """Test checking element existence."""
    client, stub = stub_client
    client.session_id = "test-session-123"
    driver = WebDriver(client)
    
    exists = driver.exists("h1.title")
    
    assert exists is True


def test_webdriver_wait_for_selector(stub_client):
    """Test waiting for selector."""
    client, stub = stub_client
    client.session_id = "test-session-123"
    driver = WebDriver(client)
    
    found = driver.wait_for_selector("h1.title", timeout=5000)
    
    assert found is True


def test_webdriver_execute_script(stub_client):
    """Test script execution."""
    client, stub = stub_client
    client.session_id = "test-session-123"
    driver = WebDriver(client)
    
    result = driver.execute_script("return document.title")
    
    assert result == "script result"


def test_webdriver_delay(stub_client):
    """Test delay control."""
    client, stub = stub_client
    client.session_id = "test-session-123"
    driver = WebDriver(client)
    
    driver.delay(1000)
    
    request_body = json.loads(stub.calls[-1]["data"])
    assert request_body["ms"] == 1000


def test_webdriver_navigate_history(stub_client):
    """Test navigation history tracking."""
    client, stub = stub_client
    client.session_id = "test-session-123"
    driver = WebDriver(client)
    
    driver.navigate_to("https://example.com")
    driver.navigate_to("https://example.com/page2")
    
    history = driver.navigate_history
    assert len(history) == 2
    assert history[0] == "https://example.com"
    assert history[1] == "https://example.com/page2"


# ========== Model Tests ==========

def test_webpage_from_dict():
    """Test WebPage creation from dictionary."""
    data = {
        "url": "https://example.com",
        "location": "https://example.com/final",
        "contentType": "text/html",
        "contentLength": 1024,
        "protocolStatus": "200 OK",
        "isNil": False
    }
    
    page = WebPage.from_dict(data)
    
    assert page.url == "https://example.com"
    assert page.location == "https://example.com/final"
    assert page.content_type == "text/html"
    assert page.content_length == 1024
    assert not page.is_nil


def test_normurl_from_dict():
    """Test NormURL creation from dictionary."""
    data = {
        "spec": "https://example.com -expire 1d",
        "url": "https://example.com",
        "args": "-expire 1d",
        "isNil": False
    }
    
    norm = NormURL.from_dict(data)
    
    assert norm.spec == "https://example.com -expire 1d"
    assert norm.url == "https://example.com"
    assert norm.args == "-expire 1d"


def test_agent_run_result_from_dict():
    """Test AgentRunResult creation from dictionary."""
    data = {
        "success": True,
        "message": "Completed",
        "historySize": 5,
        "processTraceSize": 3,
        "finalResult": {"key": "value"}
    }
    
    result = AgentRunResult.from_dict(data)
    
    assert result.success
    assert result.message == "Completed"
    assert result.history_size == 5
    assert result.final_result == {"key": "value"}
    # Test Kotlin-style alias
    assert result.finalResult == {"key": "value"}


def test_page_event_handlers_placeholder():
    """Test PageEventHandlers placeholder."""
    handlers = PageEventHandlers()
    
    assert isinstance(handlers.browse_event_handlers, dict)
    assert isinstance(handlers.load_event_handlers, dict)
    assert isinstance(handlers.crawl_event_handlers, dict)


# ========== Integration-style Tests ==========

def test_full_workflow(stub_client):
    """Test a complete workflow similar to FusedActs."""
    client, stub = stub_client
    client.session_id = "test-session-123"
    session = AgenticSession(client)
    
    # Open URL
    page = session.open("https://example.com")
    assert page.url == "https://example.com"
    
    # Get driver
    driver = session.get_or_create_bound_driver()
    assert driver is session.driver
    
    # Execute action
    result = session.act("click the search button")
    assert result.success
    
    # Run task
    history = session.run("search for 'test'")
    assert history.success
    
    # Check trace
    assert len(session.process_trace) > 0
    
    # Clear and verify
    session.clear_history()
    assert len(session.process_trace) == 0
