@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.network

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.types.network.SignedExchangeInfo
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * Fired when a signed exchange was received over the network
 */
@Experimental
data class SignedExchangeReceived(
  @param:JsonProperty("requestId")
  val requestId: String,
  @param:JsonProperty("info")
  val info: SignedExchangeInfo,
)
