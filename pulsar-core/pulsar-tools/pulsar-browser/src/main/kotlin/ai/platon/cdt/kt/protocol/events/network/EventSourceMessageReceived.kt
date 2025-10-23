@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.network

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.String

/**
 * Fired when EventSource message is received.
 */
data class EventSourceMessageReceived(
  @param:JsonProperty("requestId")
  val requestId: String,
  @param:JsonProperty("timestamp")
  val timestamp: Double,
  @param:JsonProperty("eventName")
  val eventName: String,
  @param:JsonProperty("eventId")
  val eventId: String,
  @param:JsonProperty("data")
  val `data`: String,
)
