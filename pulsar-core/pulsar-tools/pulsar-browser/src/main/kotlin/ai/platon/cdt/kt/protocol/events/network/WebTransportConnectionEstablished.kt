@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.network

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.String

/**
 * Fired when WebTransport handshake is finished.
 */
data class WebTransportConnectionEstablished(
  @param:JsonProperty("transportId")
  val transportId: String,
  @param:JsonProperty("timestamp")
  val timestamp: Double,
)
