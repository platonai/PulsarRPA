import os
import re
import shutil
import signal
import sys
import threading
from typing import ClassVar, Optional
import warnings
import importlib

from py4j.java_collections import JavaMap
from py4j.protocol import Py4JError, Py4JJavaError
from py4j.java_gateway import JavaGateway, JVMView

from pypulsar.context import PulsarContext


class PageEventHandler:
    """
    A handler for page load events.
    """
    def __init__(self, event_handler):
        """
        Initialize the PageEventHandler with a callable event handler.

        Args:
            event_handler: A callable object that handles page load events.
        """
        if not callable(event_handler):
            raise ValueError("event_handler must be a callable function")
        self.event_handler = event_handler

    def on_page_loaded(self, page):
        """
        Callback method invoked when a page is successfully loaded.

        Args:
            page: The loaded page object.
        """
        print(f"Page loaded: {page.url}")
        self.event_handler.onPageLoaded(page)

    def onPageLoadFailed(self, page):
        """
        Callback method invoked when a page load fails.

        Args:
            page: The page object that failed to load.
        """
        print(f"Page load failed: {page.url}")
        self.event_handler.onPageLoadFailed(page)


class PulsarSession:
    """
    Main entry point for Browser4 functionality.
    """

    _gateway: ClassVar[Optional[JavaGateway]] = None
    _jvm: ClassVar[Optional[JVMView]] = None
    _session: ClassVar[Optional[JVMView]] = None

    def __init__(self):
        if PulsarSession._gateway is None:
            PulsarSession._gateway = JavaGateway()
        if PulsarSession._jvm is None:
            PulsarSession._jvm = self._gateway.jvm
        if PulsarSession._session is None:
            PulsarSession._session = PulsarContext.get_or_create_session()

    def load(self, url: str, event_handler: Optional[PageEventHandler] = None):
        """
        Load a URL using the Browser4's API.
        Optionally register a callback function for page load events.

        Args:
            url (str): The URL to load.
            event_handler (Optional[PageEventHandler]): The event handler to register.

        Returns:
            The loaded page object.

        Raises:
            ValueError: If the URL is invalid or the event handler is not callable.
            RuntimeError: If the load operation fails.
        """
        if not isinstance(url, str) or not re.match(r"^https?://", url.strip()):
            raise ValueError("Invalid URL provided")

        try:
            if event_handler is not None:
                if not isinstance(event_handler, PageEventHandler):
                    raise ValueError("event_handler must be an instance of PageEventHandler")
                page = self._session.load(url, event_handler.event_handler)
            else:
                page = self._session.load(url)

            if page is None:
                raise RuntimeError(f"Failed to load URL {url}: No WebPage returned")
            return page
        except Py4JJavaError as e:
            raise RuntimeError(f"Failed to load URL {url}") from e

    def parse(self, url: str):
        pass

    def talk(self, url: str):
        pass

    def save(self, url: str):
        pass

    def close(self):
        pass
