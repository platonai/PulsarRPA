"""
AgenticSession and PulsarSession classes for browser automation.

This module provides high-level session management for browser automation,
combining page loading, parsing, extraction, and AI-powered agent capabilities.

The AgenticSession class mirrors the Kotlin AgenticSession interface, providing:
- PulsarSession methods: open, load, submit, normalize, parse, extract, scrape
- Agent methods: act, run, observe, summarize, extract (AI-powered)
- WebDriver access for low-level browser control

Usage example:
    >>> from pulsar_sdk import PulsarClient, AgenticSession
    >>> client = PulsarClient()
    >>> session_id = client.create_session()
    >>> session = AgenticSession(client)
    >>> 
    >>> # PulsarSession-like operations
    >>> page = session.open("https://example.com")
    >>> fields = session.extract(page, {"title": "h1"})
    >>> 
    >>> # Agentic operations
    >>> result = session.act("click the login button")
    >>> history = session.run("search for 'python' and click first result")
"""
from typing import Any, Dict, Iterable, List, Mapping, Optional, Union

from .client import PulsarClient
from .models import (
    AgentActResult,
    AgentObservation,
    AgentRunResult,
    ExtractionResult,
    FieldsExtraction,
    NormURL,
    ObserveResult,
    PageSnapshot,
    ToolCallResult,
    WebPage,
    PageEventHandlers,
)
from .webdriver import WebDriver


class PulsarSession:
    """
    PulsarSession provides methods for loading pages from storage or internet,
    parsing them, and extracting data.
    
    This class mirrors the Kotlin PulsarSession interface, providing a consistent
    API across languages for web scraping and data extraction tasks.
    
    Key methods:
    - open: Open a URL immediately (bypass cache)
    - load: Load from cache or fetch from internet
    - submit: Submit URL to crawl pool for async processing
    - normalize: Normalize a URL with load arguments
    - parse: Parse a page into a document
    - extract: Extract fields from a document
    - scrape: Load, parse, and extract in one operation
    """

    def __init__(self, client: PulsarClient):
        """
        Initialize PulsarSession with a PulsarClient.
        
        Args:
            client: PulsarClient instance for API communication.
        """
        self.client = client
        self._driver: Optional[WebDriver] = None
        self._id: int = 0

    @property
    def id(self) -> int:
        """Get the session ID (numeric)."""
        return self._id

    @property
    def uuid(self) -> str:
        """Get the session UUID."""
        return self.client.session_id or ""

    @property
    def display(self) -> str:
        """Get a short descriptive display text."""
        return f"PulsarSession({self.uuid[:8]}...)" if self.uuid else "PulsarSession(no-session)"

    @property
    def is_active(self) -> bool:
        """Check if the session is active."""
        return self.client.session_id is not None

    @property
    def driver(self) -> WebDriver:
        """Get the bound WebDriver instance."""
        if self._driver is None:
            self._driver = WebDriver(self.client)
        return self._driver

    @property
    def bound_driver(self) -> Optional[WebDriver]:
        """Get the bound driver (or None if not bound)."""
        return self._driver

    # ========== URL Normalization ==========

    def normalize(
        self,
        url: str,
        args: Optional[str] = None,
        to_item_option: bool = False
    ) -> NormURL:
        """
        Normalize a URL with optional load arguments.
        
        Args:
            url: The URL to normalize.
            args: Optional load arguments (e.g., "-expire 1d").
            to_item_option: Whether to convert to item load options.
            
        Returns:
            NormURL with normalized URL and parsed arguments.
        """
        payload: Dict[str, Any] = {"url": url}
        if args:
            payload["args"] = args
        if to_item_option:
            payload["toItemOption"] = to_item_option
        value = self.client.post("/session/{sessionId}/normalize", payload)
        return NormURL.from_dict(value) if isinstance(value, dict) else NormURL(spec=url, url=url)

    def normalize_or_null(
        self,
        url: Optional[str],
        args: Optional[str] = None,
        to_item_option: bool = False
    ) -> Optional[NormURL]:
        """
        Normalize a URL, returning None if invalid.
        
        Args:
            url: The URL to normalize (can be None).
            args: Optional load arguments.
            to_item_option: Whether to convert to item load options.
            
        Returns:
            NormURL or None if URL is invalid.
        """
        if not url:
            return None
        result = self.normalize(url, args, to_item_option)
        return None if result.is_nil else result

    # ========== Page Loading ==========

    def open(self, url: str, args: Optional[str] = None) -> WebPage:
        """
        Open a URL immediately, bypassing local cache.
        
        This method opens the URL immediately, regardless of the previous
        state of the page in local storage.
        
        Args:
            url: The URL to open.
            args: Optional load arguments.
            
        Returns:
            WebPage with the loaded page information.
        """
        payload: Dict[str, Any] = {"url": url}
        if args:
            payload["args"] = args
        value = self.client.post("/session/{sessionId}/open", payload)
        return WebPage.from_dict(value) if isinstance(value, dict) else WebPage(url=url)

    def load(self, url: str, args: Optional[str] = None) -> WebPage:
        """
        Load a URL from local storage or fetch from internet.
        
        This method first checks if the page exists in local storage and
        meets the specified criteria. If so, it returns the cached version.
        Otherwise, it fetches the page from the internet.
        
        Args:
            url: The URL to load.
            args: Optional load arguments (e.g., "-expire 1d", "-refresh").
            
        Returns:
            WebPage with the loaded page information.
        """
        payload: Dict[str, Any] = {"url": url}
        if args:
            payload["args"] = args
        value = self.client.post("/session/{sessionId}/load", payload)
        return WebPage.from_dict(value) if isinstance(value, dict) else WebPage(url=url)

    def load_all(
        self,
        urls: Iterable[str],
        args: Optional[str] = None
    ) -> List[WebPage]:
        """
        Load multiple URLs.
        
        Args:
            urls: Iterable of URLs to load.
            args: Optional load arguments applied to all URLs.
            
        Returns:
            List of loaded WebPages.
        """
        return [self.load(url, args) for url in urls]

    def submit(self, url: str, args: Optional[str] = None) -> bool:
        """
        Submit a URL to the crawl pool for asynchronous processing.
        
        This is a non-blocking operation that returns immediately.
        The URL will be processed later in the crawl loop.
        
        Args:
            url: The URL to submit.
            args: Optional load arguments.
            
        Returns:
            True if the URL was submitted successfully.
        """
        payload: Dict[str, Any] = {"url": url}
        if args:
            payload["args"] = args
        value = self.client.post("/session/{sessionId}/submit", payload)
        return bool(value) if value is not None else True

    def submit_all(self, urls: Iterable[str], args: Optional[str] = None) -> bool:
        """
        Submit multiple URLs to the crawl pool.
        
        Args:
            urls: Iterable of URLs to submit.
            args: Optional load arguments applied to all URLs.
            
        Returns:
            True if all URLs were submitted successfully.
        """
        for url in urls:
            if not self.submit(url, args):
                return False
        return True

    # ========== Parsing and Extraction ==========

    def parse(self, page: WebPage) -> Any:
        """
        Parse a WebPage into a document.
        
        Note: Parsing is typically done locally. This method returns the
        HTML content for local parsing with libraries like BeautifulSoup.
        
        Args:
            page: The WebPage to parse.
            
        Returns:
            HTML content for local parsing.
        """
        return page.html

    def extract(
        self,
        document: Any,
        field_selectors: Union[Mapping[str, str], Iterable[str]]
    ) -> Dict[str, Optional[str]]:
        """
        Extract fields from a document using CSS selectors.
        
        Args:
            document: The document (or page) to extract from.
            field_selectors: Either a dict mapping field names to selectors,
                           or an iterable of selectors (selector becomes field name).
            
        Returns:
            Dictionary mapping field names to extracted values.
        """
        if isinstance(field_selectors, Mapping):
            selectors = dict(field_selectors)
        else:
            selectors = {s: s for s in field_selectors}
        
        return self.driver.extract(selectors)

    def scrape(
        self,
        url: str,
        args: str,
        field_selectors: Union[Mapping[str, str], Iterable[str]]
    ) -> Dict[str, Optional[str]]:
        """
        Load a page, parse it, and extract fields in one operation.
        
        Args:
            url: The URL to scrape.
            args: Load arguments.
            field_selectors: Field selectors for extraction.
            
        Returns:
            Dictionary mapping field names to extracted values.
        """
        page = self.load(url, args)
        return self.extract(page, field_selectors)

    # ========== Driver Management ==========

    def get_or_create_bound_driver(self) -> WebDriver:
        """
        Get or create a bound WebDriver.
        
        Returns:
            The bound WebDriver instance.
        """
        return self.driver

    def create_bound_driver(self) -> WebDriver:
        """
        Create a new bound WebDriver.
        
        Returns:
            A new WebDriver instance.
        """
        self._driver = WebDriver(self.client)
        return self._driver

    def bind_driver(self, driver: WebDriver) -> None:
        """
        Bind a WebDriver to this session.
        
        Args:
            driver: The WebDriver to bind.
        """
        self._driver = driver

    def unbind_driver(self, driver: WebDriver) -> None:
        """
        Unbind a WebDriver from this session.
        
        Args:
            driver: The WebDriver to unbind.
        """
        if self._driver is driver:
            self._driver = None

    # ========== Utility Methods ==========

    def exists(self, url: str) -> bool:
        """
        Check if a page exists in storage.
        
        Args:
            url: The URL to check.
            
        Returns:
            True if the page exists in storage.
        """
        # This would need a dedicated endpoint; using a workaround
        return False

    def flush(self) -> None:
        """Flush pending changes to storage."""
        pass

    def close(self) -> None:
        """Close the session."""
        self.client.delete_session()


class AgenticSession(PulsarSession):
    """
    AgenticSession extends PulsarSession with AI-powered browser automation.
    
    This class provides methods for intelligent browser interaction using
    natural language instructions. It combines the data extraction capabilities
    of PulsarSession with AI-powered agent functionality.
    
    Key capabilities:
    - All PulsarSession methods (open, load, submit, extract, etc.)
    - Agent act: Execute single actions described in natural language
    - Agent run: Execute multi-step tasks autonomously
    - Agent observe: Analyze page and suggest actions
    - Agent extract: AI-powered data extraction
    - Agent summarize: Generate page summaries
    
    Usage example:
        >>> session = AgenticSession(client)
        >>> session.open("https://example.com")
        >>> result = session.act("click the search button")
        >>> history = session.run("search for 'python' and extract results")
    """

    def __init__(self, client: PulsarClient):
        """
        Initialize AgenticSession with a PulsarClient.
        
        Args:
            client: PulsarClient instance for API communication.
        """
        super().__init__(client)
        self._process_trace: List[str] = []

    @property
    def companion_agent(self) -> "AgenticSession":
        """
        Get the companion agent (self, for API compatibility with Kotlin).
        
        In the Kotlin implementation, companionAgent is a PerceptiveAgent.
        Here, AgenticSession itself provides the agent functionality.
        """
        return self

    @property
    def process_trace(self) -> List[str]:
        """Get the process trace (Pythonic naming)."""
        return list(self._process_trace)

    @property
    def processTrace(self) -> List[str]:
        """Get the process trace (Kotlin-style naming)."""
        return list(self._process_trace)

    @property
    def context(self) -> "AgenticSession":
        """Get the context (self, for API compatibility)."""
        return self

    # ========== Agentic Operations ==========

    def act(self, action: str, **kwargs: Any) -> AgentActResult:
        """
        Execute a single action described in natural language.
        
        This method converts the action description into browser operations
        and executes them.
        
        Args:
            action: Natural language description of the action to perform.
            **kwargs: Additional parameters (multiAct, modelName, etc.).
            
        Returns:
            AgentActResult with the action result.
        """
        return self.agent_act(action, **kwargs)

    def agent_act(self, action: str, **kwargs: Any) -> AgentActResult:
        """
        Execute a single action described in natural language.
        
        Args:
            action: Natural language description of the action.
            **kwargs: Additional parameters:
                - multiAct: Whether each act forms a new chained context.
                - modelName: Optional LLM model name.
                - variables: Extra variables for prompt/tool.
                - domSettleTimeoutMs: Timeout for DOM settling.
                - timeoutMs: Overall timeout.
                
        Returns:
            AgentActResult with the action result.
        """
        payload: Dict[str, Any] = {"action": action}
        payload.update(kwargs)
        
        value = self.client.post("/session/{sessionId}/agent/act", payload)
        
        trace = value.get("trace") if isinstance(value, dict) else None
        if trace:
            self._process_trace.extend(trace)
        
        return AgentActResult.from_dict(value) if isinstance(value, dict) else AgentActResult()

    def run(self, task: str, **kwargs: Any) -> AgentRunResult:
        """
        Run an autonomous agent task.
        
        This method runs an observe-act loop attempting to fulfill the
        task described in natural language.
        
        Args:
            task: Natural language description of the task to accomplish.
            **kwargs: Additional parameters.
            
        Returns:
            AgentRunResult with the task result.
        """
        return self.agent_run(task, **kwargs)

    def agent_run(self, task: str, **kwargs: Any) -> AgentRunResult:
        """
        Run an autonomous agent task.
        
        Args:
            task: Natural language description of the task.
            **kwargs: Additional parameters:
                - multiAct: Whether each act forms a new chained context.
                - modelName: Optional LLM model name.
                - variables: Extra variables for prompt/tool.
                - domSettleTimeoutMs: Timeout for DOM settling.
                - timeoutMs: Overall timeout.
                
        Returns:
            AgentRunResult with the task result.
        """
        payload: Dict[str, Any] = {"task": task}
        payload.update(kwargs)
        
        value = self.client.post("/session/{sessionId}/agent/run", payload)
        
        trace = value.get("trace") if isinstance(value, dict) else None
        if trace:
            self._process_trace.extend(trace)
        
        return AgentRunResult.from_dict(value) if isinstance(value, dict) else AgentRunResult()

    def observe(self, instruction: Optional[str] = None, **kwargs: Any) -> AgentObservation:
        """
        Observe the page and return potential actions.
        
        Args:
            instruction: Optional observation instruction.
            **kwargs: Additional parameters.
            
        Returns:
            AgentObservation with observation results.
        """
        return self.agent_observe(instruction, **kwargs)

    def agent_observe(self, instruction: Optional[str] = None, **kwargs: Any) -> AgentObservation:
        """
        Observe the page and return potential actions.
        
        Args:
            instruction: Optional observation instruction.
            **kwargs: Additional parameters:
                - modelName: Optional LLM model name.
                - domSettleTimeoutMs: Timeout for DOM settling.
                - returnAction: Whether to return actionable tool calls.
                - drawOverlay: Whether to highlight interactive elements.
                
        Returns:
            AgentObservation with observation results.
        """
        payload: Dict[str, Any] = {}
        if instruction:
            payload["instruction"] = instruction
        payload.update(kwargs)
        
        value = self.client.post("/session/{sessionId}/agent/observe", payload)
        return AgentObservation.from_dict(value)

    def agent_extract(
        self,
        instruction: str,
        schema: Optional[Dict[str, Any]] = None,
        selector: Optional[str] = None,
        **kwargs: Any
    ) -> ExtractionResult:
        """
        Extract structured data from the page using AI.
        
        Args:
            instruction: Extraction instruction describing what to extract.
            schema: Optional JSON schema for the extraction result.
            selector: Optional CSS selector to scope extraction.
            **kwargs: Additional parameters.
            
        Returns:
            ExtractionResult with extracted data.
        """
        payload: Dict[str, Any] = {"instruction": instruction}
        if schema:
            payload["schema"] = schema
        if selector:
            payload["selector"] = selector
        payload.update(kwargs)
        
        value = self.client.post("/session/{sessionId}/agent/extract", payload)
        return ExtractionResult.from_dict(value) if isinstance(value, dict) else ExtractionResult()

    def summarize(self, instruction: Optional[str] = None, selector: Optional[str] = None) -> str:
        """
        Summarize page content.
        
        Args:
            instruction: Optional guidance for summarization.
            selector: Optional CSS selector to limit summarization scope.
            
        Returns:
            Summary text.
        """
        return self.agent_summarize(instruction, selector)

    def agent_summarize(self, instruction: Optional[str] = None, selector: Optional[str] = None) -> str:
        """
        Summarize page content.
        
        Args:
            instruction: Optional guidance for summarization.
            selector: Optional CSS selector to limit summarization scope.
            
        Returns:
            Summary text.
        """
        payload: Dict[str, Any] = {}
        if instruction:
            payload["instruction"] = instruction
        if selector:
            payload["selector"] = selector
        
        value = self.client.post("/session/{sessionId}/agent/summarize", payload)
        if isinstance(value, dict):
            return value.get("summary", value.get("value", ""))
        return str(value) if value else ""

    def clear_history(self) -> bool:
        """
        Clear the agent's history.
        
        This clears the history so new tasks remain unaffected by previous ones.
        
        Returns:
            True if history was cleared successfully.
        """
        return self.agent_clear_history()

    def agent_clear_history(self) -> bool:
        """
        Clear the agent's history.
        
        Returns:
            True if history was cleared successfully.
        """
        value = self.client.post("/session/{sessionId}/agent/clearHistory", {})
        self._process_trace.clear()
        return bool(value) if value is not None else True

    # ========== Capture Operations ==========

    def capture(self, driver: Optional[WebDriver] = None, url: Optional[str] = None) -> PageSnapshot:
        """
        Capture the live page controlled by a WebDriver.
        
        This creates a static snapshot of the current page state.
        
        Args:
            driver: The WebDriver controlling the page (uses bound driver if None).
            url: Optional URL to identify the capture.
            
        Returns:
            PageSnapshot with the captured page.
        """
        drv = driver or self.driver
        current_url = url or drv.get_current_url()
        value = self.client.post("/session/{sessionId}/open", {"url": current_url})
        return PageSnapshot(
            url=value.get("url", current_url) if isinstance(value, dict) else current_url,
            html=value.get("html") if isinstance(value, dict) else None
        )

    # ========== Helper Methods (Kotlin API Compatibility) ==========

    def register_closable(self, closable: Any) -> None:
        """
        Register a closable object with the session.
        
        Args:
            closable: Object with a close() method.
        """
        # Placeholder for resource management
        pass

    def data(self, name: str, value: Any = None) -> Any:
        """
        Get or set session data.
        
        Args:
            name: Data key name.
            value: Value to set (if provided).
            
        Returns:
            Stored value for the name.
        """
        # Placeholder for session data storage
        return None

    def property(self, name: str, value: Optional[str] = None) -> Optional[str]:
        """
        Get or set a session property.
        
        Args:
            name: Property name.
            value: Value to set (if provided).
            
        Returns:
            Property value.
        """
        # Placeholder for session properties
        return None

    def options(self, args: str = "", event_handlers: Optional[PageEventHandlers] = None) -> Dict[str, Any]:
        """
        Create load options from arguments string.
        
        Args:
            args: Load arguments string.
            event_handlers: Optional event handlers.
            
        Returns:
            Options dictionary.
        """
        return {"args": args, "eventHandlers": event_handlers}

    def close(self) -> None:
        """Close the session and release resources."""
        self._process_trace.clear()
        self.client.delete_session()


__all__ = ["PulsarSession", "AgenticSession"]
