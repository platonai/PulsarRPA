@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.network

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.Double
import kotlin.String

/**
 * WebSocket message data. This represents an entire WebSocket message, not just a fragmented frame as the name suggests.
 */
data class WebSocketFrame(
  @param:JsonProperty("opcode")
  val opcode: Double,
  @param:JsonProperty("mask")
  val mask: Boolean,
  @param:JsonProperty("payloadData")
  val payloadData: String,
)
