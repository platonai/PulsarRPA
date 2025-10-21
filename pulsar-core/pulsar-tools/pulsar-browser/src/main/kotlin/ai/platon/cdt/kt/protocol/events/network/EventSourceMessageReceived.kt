package ai.platon.cdt.kt.protocol.events.network

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.String

/**
 * Fired when EventSource message is received.
 */
public data class EventSourceMessageReceived(
  @JsonProperty("requestId")
  public val requestId: String,
  @JsonProperty("timestamp")
  public val timestamp: Double,
  @JsonProperty("eventName")
  public val eventName: String,
  @JsonProperty("eventId")
  public val eventId: String,
  @JsonProperty("data")
  public val `data`: String,
)
