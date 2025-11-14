@file:Suppress("unused")
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
data class ResponseReceived(
  @param:JsonProperty("requestId")
  val requestId: String,
  @param:JsonProperty("loaderId")
  val loaderId: String,
  @param:JsonProperty("timestamp")
  val timestamp: Double,
  @param:JsonProperty("type")
  val type: ResourceType,
  @param:JsonProperty("response")
  val response: Response,
  @param:JsonProperty("frameId")
  @param:Optional
  val frameId: String? = null,
)
