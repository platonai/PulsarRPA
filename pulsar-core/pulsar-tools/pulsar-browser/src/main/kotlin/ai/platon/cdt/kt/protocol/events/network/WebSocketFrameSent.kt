package ai.platon.cdt.kt.protocol.events.network

import ai.platon.cdt.kt.protocol.types.network.WebSocketFrame
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.String

/**
 * Fired when WebSocket message is sent.
 */
public data class WebSocketFrameSent(
  @JsonProperty("requestId")
  public val requestId: String,
  @JsonProperty("timestamp")
  public val timestamp: Double,
  @JsonProperty("response")
  public val response: WebSocketFrame,
)
