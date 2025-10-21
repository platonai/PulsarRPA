package ai.platon.cdt.kt.protocol.events.network

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.types.network.Initiator
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.String

/**
 * Fired upon WebTransport creation.
 */
public data class WebTransportCreated(
  @JsonProperty("transportId")
  public val transportId: String,
  @JsonProperty("url")
  public val url: String,
  @JsonProperty("timestamp")
  public val timestamp: Double,
  @JsonProperty("initiator")
  @Optional
  public val initiator: Initiator? = null,
)
