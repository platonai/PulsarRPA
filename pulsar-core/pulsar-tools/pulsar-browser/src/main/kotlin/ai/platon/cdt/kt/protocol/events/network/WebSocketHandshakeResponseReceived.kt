package ai.platon.cdt.kt.protocol.events.network

import ai.platon.cdt.kt.protocol.types.network.WebSocketResponse
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.String

/**
 * Fired when WebSocket handshake response becomes available.
 */
public data class WebSocketHandshakeResponseReceived(
  @JsonProperty("requestId")
  public val requestId: String,
  @JsonProperty("timestamp")
  public val timestamp: Double,
  @JsonProperty("response")
  public val response: WebSocketResponse,
)
