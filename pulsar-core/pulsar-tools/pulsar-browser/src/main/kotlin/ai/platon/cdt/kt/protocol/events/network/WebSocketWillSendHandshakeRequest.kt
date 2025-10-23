@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.network

import ai.platon.cdt.kt.protocol.types.network.WebSocketRequest
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.String

/**
 * Fired when WebSocket is about to initiate handshake.
 */
data class WebSocketWillSendHandshakeRequest(
  @param:JsonProperty("requestId")
  val requestId: String,
  @param:JsonProperty("timestamp")
  val timestamp: Double,
  @param:JsonProperty("wallTime")
  val wallTime: Double,
  @param:JsonProperty("request")
  val request: WebSocketRequest,
)
