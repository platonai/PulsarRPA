package ai.platon.cdt.kt.protocol.types.fetch

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * Authorization challenge for HTTP status code 401 or 407.
 */
public data class AuthChallenge(
  @JsonProperty("source")
  @Optional
  public val source: AuthChallengeSource? = null,
  @JsonProperty("origin")
  public val origin: String,
  @JsonProperty("scheme")
  public val scheme: String,
  @JsonProperty("realm")
  public val realm: String,
)
