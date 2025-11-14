@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.network

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.types.network.Initiator
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * Fired upon WebSocket creation.
 */
data class WebSocketCreated(
  @param:JsonProperty("requestId")
  val requestId: String,
  @param:JsonProperty("url")
  val url: String,
  @param:JsonProperty("initiator")
  @param:Optional
  val initiator: Initiator? = null,
)
