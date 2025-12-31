from dataclasses import dataclass
from typing import Any, Dict, List, Optional


@dataclass
class ElementRef:
    element_id: str


@dataclass
class AgentRunResult:
    finalResult: Any
    trace: Optional[List[str]] = None


@dataclass
class AgentActResult:
    result: Any
    trace: Optional[List[str]] = None


@dataclass
class AgentObservation:
    observations: Any


@dataclass
class ExtractionResult:
    data: Any


@dataclass
class PageSnapshot:
    url: str
    html: Optional[str] = None


@dataclass
class FieldsExtraction:
    fields: Dict[str, Any]


__all__ = [
    "ElementRef",
    "AgentRunResult",
    "AgentActResult",
    "AgentObservation",
    "ExtractionResult",
    "PageSnapshot",
    "FieldsExtraction",
]
