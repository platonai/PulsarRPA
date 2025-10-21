package ai.platon.cdt.kt.protocol.events.network

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.types.network.ResourceType
import ai.platon.cdt.kt.protocol.types.network.Response
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.String

/**
 * Fired when HTTP response is available.
 */
public data class ResponseReceived(
  @JsonProperty("requestId")
  public val requestId: String,
  @JsonProperty("loaderId")
  public val loaderId: String,
  @JsonProperty("timestamp")
  public val timestamp: Double,
  @JsonProperty("type")
  public val type: ResourceType,
  @JsonProperty("response")
  public val response: Response,
  @JsonProperty("frameId")
  @Optional
  public val frameId: String? = null,
)
