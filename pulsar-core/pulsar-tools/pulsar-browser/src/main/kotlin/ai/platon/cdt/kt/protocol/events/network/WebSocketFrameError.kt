package ai.platon.cdt.kt.protocol.events.network

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.String

/**
 * Fired when WebSocket message error occurs.
 */
public data class WebSocketFrameError(
  @JsonProperty("requestId")
  public val requestId: String,
  @JsonProperty("timestamp")
  public val timestamp: Double,
  @JsonProperty("errorMessage")
  public val errorMessage: String,
)
