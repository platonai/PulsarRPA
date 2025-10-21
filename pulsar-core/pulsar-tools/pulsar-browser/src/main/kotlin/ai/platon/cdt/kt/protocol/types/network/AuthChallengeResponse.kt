package ai.platon.cdt.kt.protocol.types.network

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * Response to an AuthChallenge.
 */
@Experimental
public data class AuthChallengeResponse(
  @JsonProperty("response")
  public val response: AuthChallengeResponseResponse,
  @JsonProperty("username")
  @Optional
  public val username: String? = null,
  @JsonProperty("password")
  @Optional
  public val password: String? = null,
)
