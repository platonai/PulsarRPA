package ai.platon.cdt.kt.protocol.events.network

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.String

/**
 * Fired when WebTransport is disposed.
 */
public data class WebTransportClosed(
  @JsonProperty("transportId")
  public val transportId: String,
  @JsonProperty("timestamp")
  public val timestamp: Double,
)
