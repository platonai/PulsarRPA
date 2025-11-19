@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.fetch

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * Authorization challenge for HTTP status code 401 or 407.
 */
data class AuthChallenge(
  @param:JsonProperty("source")
  @param:Optional
  val source: AuthChallengeSource? = null,
  @param:JsonProperty("origin")
  val origin: String,
  @param:JsonProperty("scheme")
  val scheme: String,
  @param:JsonProperty("realm")
  val realm: String,
)
