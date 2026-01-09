"""
Data models for the Python SDK.

These models correspond to the Kotlin data classes and provide a consistent
interface for working with Browser4 API responses.
"""
from dataclasses import dataclass, field
from typing import Any, Dict, List, Optional


@dataclass
class ElementRef:
    """Reference to a DOM element, matching WebDriver element identifier."""
    element_id: str


@dataclass
class WebPage:
    """
    Represents a web page result from load/open operations.
    Mirrors the Kotlin WebPage class.
    """
    url: str
    location: Optional[str] = None
    content_type: Optional[str] = None
    content_length: int = 0
    protocol_status: Optional[str] = None
    is_nil: bool = False
    html: Optional[str] = None

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> "WebPage":
        """Create WebPage from API response dict."""
        return cls(
            url=data.get("url", ""),
            location=data.get("location"),
            content_type=data.get("contentType"),
            content_length=data.get("contentLength", 0),
            protocol_status=data.get("protocolStatus"),
            is_nil=data.get("isNil", False),
            html=data.get("html"),
        )


@dataclass
class NormURL:
    """
    Normalized URL result.
    Mirrors the Kotlin NormURL class.
    """
    spec: str
    url: str
    args: Optional[str] = None
    is_nil: bool = False

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> "NormURL":
        """Create NormURL from API response dict."""
        return cls(
            spec=data.get("spec", ""),
            url=data.get("url", ""),
            args=data.get("args"),
            is_nil=data.get("isNil", False),
        )


@dataclass
class AgentRunResult:
    """Result from agent run operation."""
    success: bool = False
    message: str = ""
    history_size: int = 0
    process_trace_size: int = 0
    final_result: Any = None
    trace: Optional[List[str]] = None

    # Kotlin-style alias
    @property
    def finalResult(self) -> Any:
        return self.final_result

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> "AgentRunResult":
        """Create AgentRunResult from API response dict."""
        return cls(
            success=data.get("success", False),
            message=data.get("message", ""),
            history_size=data.get("historySize", 0),
            process_trace_size=data.get("processTraceSize", 0),
            final_result=data.get("finalResult"),
            trace=data.get("trace"),
        )


@dataclass
class AgentActResult:
    """Result from agent act operation."""
    success: bool = False
    message: str = ""
    action: Optional[str] = None
    is_complete: bool = False
    expression: Optional[str] = None
    result: Any = None
    trace: Optional[List[str]] = None

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> "AgentActResult":
        """Create AgentActResult from API response dict."""
        return cls(
            success=data.get("success", False),
            message=data.get("message", ""),
            action=data.get("action"),
            is_complete=data.get("isComplete", False),
            expression=data.get("expression"),
            result=data.get("result"),
            trace=data.get("trace"),
        )


@dataclass
class ObserveResult:
    """Single observation result from agent observe operation."""
    locator: Optional[str] = None
    domain: Optional[str] = None
    method: Optional[str] = None
    arguments: Optional[Dict[str, Any]] = None
    description: Optional[str] = None
    screenshot_content_summary: Optional[str] = None
    current_page_content_summary: Optional[str] = None
    next_goal: Optional[str] = None
    thinking: Optional[str] = None
    summary: Optional[str] = None
    key_findings: Optional[str] = None
    next_suggestions: Optional[List[str]] = None

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> "ObserveResult":
        """Create ObserveResult from API response dict."""
        return cls(
            locator=data.get("locator"),
            domain=data.get("domain"),
            method=data.get("method"),
            arguments=data.get("arguments"),
            description=data.get("description"),
            screenshot_content_summary=data.get("screenshotContentSummary"),
            current_page_content_summary=data.get("currentPageContentSummary"),
            next_goal=data.get("nextGoal"),
            thinking=data.get("thinking"),
            summary=data.get("summary"),
            key_findings=data.get("keyFindings"),
            next_suggestions=data.get("nextSuggestions"),
        )


@dataclass
class AgentObservation:
    """Result from agent observe operation."""
    observations: List[ObserveResult] = field(default_factory=list)

    @classmethod
    def from_dict(cls, data: Any) -> "AgentObservation":
        """Create AgentObservation from API response."""
        if isinstance(data, list):
            observations = [ObserveResult.from_dict(item) if isinstance(item, dict) else item for item in data]
            return cls(observations=observations)
        return cls(observations=[])


@dataclass
class ExtractionResult:
    """Result from agent extract operation."""
    success: bool = False
    message: str = ""
    data: Any = None

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> "ExtractionResult":
        """Create ExtractionResult from API response dict."""
        return cls(
            success=data.get("success", False),
            message=data.get("message", ""),
            data=data.get("data"),
        )


@dataclass
class PageSnapshot:
    """Snapshot of a web page, used for capture operations."""
    url: str
    html: Optional[str] = None


@dataclass
class FieldsExtraction:
    """Result of field extraction with CSS selectors."""
    fields: Dict[str, Any] = field(default_factory=dict)


@dataclass
class ToolCallResult:
    """
    Result of a tool call execution.
    Mirrors the Kotlin ToolCallResult class.
    """
    success: bool = False
    message: str = ""
    data: Any = None

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> "ToolCallResult":
        """Create ToolCallResult from API response dict."""
        return cls(
            success=data.get("success", False),
            message=data.get("message", ""),
            data=data.get("data"),
        )


@dataclass
class ActionDescription:
    """
    Description of an action to be performed.
    Mirrors the Kotlin ActionDescription class.
    """
    description: str
    parameters: Optional[Dict[str, Any]] = None


# Placeholder for event mechanism (to be implemented in future tasks)
class PageEventHandlers:
    """
    Placeholder for page event handlers.
    
    This class will be implemented in future tasks to support event-driven
    page interactions similar to the Kotlin PageEventHandlers interface.
    
    Future implementation will include:
    - Browse event handlers (onWillNavigate, onDocumentSteady, etc.)
    - Load event handlers (onLoaded, onHTMLDocumentParsed, etc.)
    - Crawl event handlers
    """
    
    def __init__(self):
        self._browse_event_handlers = {}
        self._load_event_handlers = {}
        self._crawl_event_handlers = {}
    
    @property
    def browse_event_handlers(self) -> Dict[str, Any]:
        """Get browse event handlers (placeholder)."""
        return self._browse_event_handlers
    
    @property
    def load_event_handlers(self) -> Dict[str, Any]:
        """Get load event handlers (placeholder)."""
        return self._load_event_handlers
    
    @property
    def crawl_event_handlers(self) -> Dict[str, Any]:
        """Get crawl event handlers (placeholder)."""
        return self._crawl_event_handlers


__all__ = [
    "ElementRef",
    "WebPage",
    "NormURL",
    "AgentRunResult",
    "AgentActResult",
    "ObserveResult",
    "AgentObservation",
    "ExtractionResult",
    "PageSnapshot",
    "FieldsExtraction",
    "ToolCallResult",
    "ActionDescription",
    "PageEventHandlers",
]
