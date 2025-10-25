@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.network

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.types.network.BlockedCookieWithReason
import ai.platon.cdt.kt.protocol.types.network.ClientSecurityState
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Any
import kotlin.String
import kotlin.collections.List
import kotlin.collections.Map

/**
 * Fired when additional information about a requestWillBeSent event is available from the
 * network stack. Not every requestWillBeSent event will have an additional
 * requestWillBeSentExtraInfo fired for it, and there is no guarantee whether requestWillBeSent
 * or requestWillBeSentExtraInfo will be fired first for the same request.
 */
@Experimental
data class RequestWillBeSentExtraInfo(
  @param:JsonProperty("requestId")
  val requestId: String,
  @param:JsonProperty("associatedCookies")
  val associatedCookies: List<BlockedCookieWithReason>,
  @param:JsonProperty("headers")
  val headers: Map<String, Any?>,
  @param:JsonProperty("clientSecurityState")
  @param:Optional
  val clientSecurityState: ClientSecurityState? = null,
)
