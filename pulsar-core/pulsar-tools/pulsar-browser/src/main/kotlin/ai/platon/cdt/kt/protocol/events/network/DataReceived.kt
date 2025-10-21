package ai.platon.cdt.kt.protocol.events.network

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.Int
import kotlin.String

/**
 * Fired when data chunk was received over the network.
 */
public data class DataReceived(
  @JsonProperty("requestId")
  public val requestId: String,
  @JsonProperty("timestamp")
  public val timestamp: Double,
  @JsonProperty("dataLength")
  public val dataLength: Int,
  @JsonProperty("encodedDataLength")
  public val encodedDataLength: Int,
)
