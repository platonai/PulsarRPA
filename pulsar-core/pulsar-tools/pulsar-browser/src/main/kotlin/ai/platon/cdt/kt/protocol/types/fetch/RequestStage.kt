package ai.platon.cdt.kt.protocol.types.fetch

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Stages of the request to handle. Request will intercept before the request is
 * sent. Response will intercept after the response is received (but before response
 * body is received.
 */
public enum class RequestStage {
  @JsonProperty("Request")
  REQUEST,
  @JsonProperty("Response")
  RESPONSE,
}
