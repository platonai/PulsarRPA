package ai.platon.cdt.kt.protocol.types.network

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Stages of the interception to begin intercepting. Request will intercept before the request is
 * sent. Response will intercept after the response is received.
 */
public enum class InterceptionStage {
  @JsonProperty("Request")
  REQUEST,
  @JsonProperty("HeadersReceived")
  HEADERS_RECEIVED,
}
