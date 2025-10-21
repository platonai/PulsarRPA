package ai.platon.cdt.kt.protocol.types.network

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Any
import kotlin.Int
import kotlin.String
import kotlin.collections.Map

/**
 * WebSocket response data.
 */
public data class WebSocketResponse(
  @JsonProperty("status")
  public val status: Int,
  @JsonProperty("statusText")
  public val statusText: String,
  @JsonProperty("headers")
  public val headers: Map<String, Any?>,
  @JsonProperty("headersText")
  @Optional
  public val headersText: String? = null,
  @JsonProperty("requestHeaders")
  @Optional
  public val requestHeaders: Map<String, Any?>? = null,
  @JsonProperty("requestHeadersText")
  @Optional
  public val requestHeadersText: String? = null,
)
