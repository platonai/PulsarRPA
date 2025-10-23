@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.network

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.types.network.Initiator
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.String

/**
 * Fired upon WebTransport creation.
 */
data class WebTransportCreated(
  @param:JsonProperty("transportId")
  val transportId: String,
  @param:JsonProperty("url")
  val url: String,
  @param:JsonProperty("timestamp")
  val timestamp: Double,
  @param:JsonProperty("initiator")
  @param:Optional
  val initiator: Initiator? = null,
)
