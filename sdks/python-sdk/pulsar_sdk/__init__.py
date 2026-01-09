"""
Python SDK for Browser4 AgenticSession and WebDriver-compatible API.

This SDK provides a Python interface to the Browser4 browser automation platform,
enabling web scraping, data extraction, and AI-powered browser interaction.

Key classes:
- PulsarClient: Low-level HTTP client for API communication
- PulsarSession: Session management for page loading and extraction
- AgenticSession: AI-powered browser automation (extends PulsarSession)
- WebDriver: Browser control and element interaction

Quick start:
    >>> from pulsar_sdk import PulsarClient, AgenticSession
    >>> 
    >>> # Create client and session
    >>> client = PulsarClient(base_url="http://localhost:8182")
    >>> session_id = client.create_session()
    >>> session = AgenticSession(client)
    >>> 
    >>> # Navigate and interact
    >>> session.driver.navigate_to("https://example.com")
    >>> print(session.driver.get_current_url())
    >>> 
    >>> # Use AI-powered actions
    >>> result = session.run("scroll to the bottom of the page")
    >>> print(result.success)
    >>> 
    >>> # Clean up
    >>> session.close()

See the README.md for more detailed usage examples.
"""

from .client import PulsarClient
from .agentic_session import PulsarSession, AgenticSession
from .webdriver import WebDriver
from .models import (
    # Core data models
    WebPage,
    NormURL,
    PageSnapshot,
    ElementRef,
    FieldsExtraction,
    # Agent result models
    AgentRunResult,
    AgentActResult,
    AgentObservation,
    ObserveResult,
    ExtractionResult,
    ToolCallResult,
    ActionDescription,
    # Event system (placeholder)
    PageEventHandlers,
)

__all__ = [
    # Client
    "PulsarClient",
    # Sessions
    "PulsarSession",
    "AgenticSession",
    # WebDriver
    "WebDriver",
    # Core models
    "WebPage",
    "NormURL",
    "PageSnapshot",
    "ElementRef",
    "FieldsExtraction",
    # Agent models
    "AgentRunResult",
    "AgentActResult",
    "AgentObservation",
    "ObserveResult",
    "ExtractionResult",
    "ToolCallResult",
    "ActionDescription",
    # Events
    "PageEventHandlers",
]

__version__ = "0.1.0"
