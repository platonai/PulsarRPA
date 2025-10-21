package ai.platon.cdt.kt.protocol.types.network

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String
import kotlin.collections.List

/**
 * A cookie which was not stored from a response with the corresponding reason.
 */
@Experimental
public data class BlockedSetCookieWithReason(
  @JsonProperty("blockedReasons")
  public val blockedReasons: List<SetCookieBlockedReason>,
  @JsonProperty("cookieLine")
  public val cookieLine: String,
  @JsonProperty("cookie")
  @Optional
  public val cookie: Cookie? = null,
)
