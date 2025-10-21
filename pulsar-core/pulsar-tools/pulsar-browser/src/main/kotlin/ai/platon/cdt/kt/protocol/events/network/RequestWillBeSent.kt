package ai.platon.cdt.kt.protocol.events.network

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.types.network.Initiator
import ai.platon.cdt.kt.protocol.types.network.Request
import ai.platon.cdt.kt.protocol.types.network.ResourceType
import ai.platon.cdt.kt.protocol.types.network.Response
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.Double
import kotlin.String

/**
 * Fired when page is about to send HTTP request.
 */
public data class RequestWillBeSent(
  @JsonProperty("requestId")
  public val requestId: String,
  @JsonProperty("loaderId")
  public val loaderId: String,
  @JsonProperty("documentURL")
  public val documentURL: String,
  @JsonProperty("request")
  public val request: Request,
  @JsonProperty("timestamp")
  public val timestamp: Double,
  @JsonProperty("wallTime")
  public val wallTime: Double,
  @JsonProperty("initiator")
  public val initiator: Initiator,
  @JsonProperty("redirectResponse")
  @Optional
  public val redirectResponse: Response? = null,
  @JsonProperty("type")
  @Optional
  public val type: ResourceType? = null,
  @JsonProperty("frameId")
  @Optional
  public val frameId: String? = null,
  @JsonProperty("hasUserGesture")
  @Optional
  public val hasUserGesture: Boolean? = null,
)
