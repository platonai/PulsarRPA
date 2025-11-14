@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.network

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Any
import kotlin.String
import kotlin.collections.Map

/**
 * WebSocket request data.
 */
data class WebSocketRequest(
  @param:JsonProperty("headers")
  val headers: Map<String, Any?>,
)
