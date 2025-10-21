package ai.platon.cdt.kt.protocol.events.network

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.types.network.BlockedSetCookieWithReason
import ai.platon.cdt.kt.protocol.types.network.IPAddressSpace
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Any
import kotlin.String
import kotlin.collections.List
import kotlin.collections.Map

/**
 * Fired when additional information about a responseReceived event is available from the network
 * stack. Not every responseReceived event will have an additional responseReceivedExtraInfo for
 * it, and responseReceivedExtraInfo may be fired before or after responseReceived.
 */
@Experimental
public data class ResponseReceivedExtraInfo(
  @JsonProperty("requestId")
  public val requestId: String,
  @JsonProperty("blockedCookies")
  public val blockedCookies: List<BlockedSetCookieWithReason>,
  @JsonProperty("headers")
  public val headers: Map<String, Any?>,
  @JsonProperty("resourceIPAddressSpace")
  public val resourceIPAddressSpace: IPAddressSpace,
  @JsonProperty("headersText")
  @Optional
  public val headersText: String? = null,
)
