@file:Suppress("unused")
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
data class RequestWillBeSent(
  @param:JsonProperty("requestId")
  val requestId: String,
  @param:JsonProperty("loaderId")
  val loaderId: String,
  @param:JsonProperty("documentURL")
  val documentURL: String,
  @param:JsonProperty("request")
  val request: Request,
  @param:JsonProperty("timestamp")
  val timestamp: Double,
  @param:JsonProperty("wallTime")
  val wallTime: Double,
  @param:JsonProperty("initiator")
  val initiator: Initiator,
  @param:JsonProperty("redirectResponse")
  @param:Optional
  val redirectResponse: Response? = null,
  @param:JsonProperty("type")
  @param:Optional
  val type: ResourceType? = null,
  @param:JsonProperty("frameId")
  @param:Optional
  val frameId: String? = null,
  @param:JsonProperty("hasUserGesture")
  @param:Optional
  val hasUserGesture: Boolean? = null,
)
