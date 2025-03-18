import os
import shutil
import signal
import sys
import threading
from typing import ClassVar, Optional
import warnings
import importlib

from py4j.java_collections import JavaMap
from py4j.protocol import Py4JError
from py4j.java_gateway import JavaGateway, JVMView

class PulsarContext:
    """
    Main entry point for PulsarRPA functionality.
    """

    _gateway: ClassVar[Optional[JavaGateway]] = None
    _jvm: ClassVar[Optional[JVMView]] = None
    _context: ClassVar[Optional[JVMView]] = None

    def __init__(self):
        if self._gateway is None:
            self._gateway = JavaGateway()
        if self._jvm is None:
            self._jvm = self._gateway.jvm
        if self._context is None:
            self._context = self._jvm.ai.platon.pulsar.skeleton.context.PulsarContexts.create()
        pass

    def load(self, url: str):
        """
        Load a url using the PulsarRPA's API.
        Call kotlin code to load the url.
        """
        self._context.load(url)
        pass

    def save(self, url: str):
        pass

    def close(self):
        pass
