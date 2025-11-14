@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.network

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Resource type as it was perceived by the rendering engine.
 */
public enum class ResourceType {
  @JsonProperty("Document")
  DOCUMENT,
  @JsonProperty("Stylesheet")
  STYLESHEET,
  @JsonProperty("Image")
  IMAGE,
  @JsonProperty("Media")
  MEDIA,
  @JsonProperty("Font")
  FONT,
  @JsonProperty("Script")
  SCRIPT,
  @JsonProperty("TextTrack")
  TEXT_TRACK,
  @JsonProperty("XHR")
  XHR,
  @JsonProperty("Fetch")
  FETCH,
  @JsonProperty("EventSource")
  EVENT_SOURCE,
  @JsonProperty("WebSocket")
  WEB_SOCKET,
  @JsonProperty("Manifest")
  MANIFEST,
  @JsonProperty("SignedExchange")
  SIGNED_EXCHANGE,
  @JsonProperty("Ping")
  PING,
  @JsonProperty("CSPViolationReport")
  CSP_VIOLATION_REPORT,
  @JsonProperty("Preflight")
  PREFLIGHT,
  @JsonProperty("Other")
  OTHER,
}
