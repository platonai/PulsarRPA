@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.network

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.String

/**
 * Fired when WebSocket is closed.
 */
data class WebSocketClosed(
  @param:JsonProperty("requestId")
  val requestId: String,
  @param:JsonProperty("timestamp")
  val timestamp: Double,
)
