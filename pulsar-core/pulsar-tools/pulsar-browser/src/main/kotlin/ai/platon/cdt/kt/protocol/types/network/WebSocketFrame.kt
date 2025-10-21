package ai.platon.cdt.kt.protocol.types.network

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.Double
import kotlin.String

/**
 * WebSocket message data. This represents an entire WebSocket message, not just a fragmented frame
 * as the name suggests.
 */
public data class WebSocketFrame(
  @JsonProperty("opcode")
  public val opcode: Double,
  @JsonProperty("mask")
  public val mask: Boolean,
  @JsonProperty("payloadData")
  public val payloadData: String,
)
