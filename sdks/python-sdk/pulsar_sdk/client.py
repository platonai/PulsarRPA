import json
from typing import Any, Dict, Optional

import requests


class PulsarClient:
    """Thin HTTP client over the Browser4 OpenAPI."""

    def __init__(
        self,
        base_url: str = "http://localhost:8182",
        timeout: float = 30.0,
        session_id: Optional[str] = None,
        default_headers: Optional[Dict[str, str]] = None,
    ):
        self.base_url = base_url.rstrip("/")
        self.timeout = timeout
        self.session = requests.Session()
        self.session_id = session_id
        self._default_headers = {"Content-Type": "application/json"}
        if default_headers:
            self._default_headers.update(default_headers)

    def _url(self, path: str) -> str:
        return f"{self.base_url}{path}"

    def _require_session(self, session_id: Optional[str]) -> str:
        sid = session_id or self.session_id
        if not sid:
            raise ValueError("session_id is required; call create_session() first or pass session_id explicitly")
        return sid

    def _request(self, method: str, path: str, *, session_id: Optional[str] = None, body: Optional[Dict[str, Any]] = None) -> Any:
        sid = self._require_session(session_id) if "{sessionId}" in path else session_id or self.session_id
        if sid:
            path = path.format(sessionId=sid)
        res = self.session.request(
            method=method,
            url=self._url(path),
            headers=self._default_headers,
            data=json.dumps(body) if body is not None else None,
            timeout=self.timeout,
        )
        res.raise_for_status()
        if not res.content:
            return None
        try:
            payload = res.json()
        except ValueError:
            return res.content
        return payload.get("value") if isinstance(payload, dict) else payload

    def create_session(self, capabilities: Optional[Dict[str, Any]] = None) -> str:
        value = self._request("POST", "/session", body={"capabilities": capabilities or {}})
        session_id = value["sessionId"] if isinstance(value, dict) else None
        if not session_id:
            raise RuntimeError("createSession response missing sessionId")
        self.session_id = session_id
        return session_id

    def delete_session(self, session_id: Optional[str] = None) -> None:
        sid = self._require_session(session_id)
        self._request("DELETE", f"/session/{sid}")

    def post(self, path: str, body: Dict[str, Any], session_id: Optional[str] = None) -> Any:
        return self._request("POST", path, session_id=session_id, body=body)

    def get(self, path: str, session_id: Optional[str] = None) -> Any:
        return self._request("GET", path, session_id=session_id)

    def close(self):
        self.session.close()


__all__ = ["PulsarClient"]

