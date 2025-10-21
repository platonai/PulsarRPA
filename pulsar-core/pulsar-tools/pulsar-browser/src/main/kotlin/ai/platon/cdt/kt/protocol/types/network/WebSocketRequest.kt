package ai.platon.cdt.kt.protocol.types.network

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Any
import kotlin.String
import kotlin.collections.Map

/**
 * WebSocket request data.
 */
public data class WebSocketRequest(
  @JsonProperty("headers")
  public val headers: Map<String, Any?>,
)
