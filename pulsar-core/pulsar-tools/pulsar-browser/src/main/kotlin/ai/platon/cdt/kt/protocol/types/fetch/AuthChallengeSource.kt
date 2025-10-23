@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.fetch

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Source of the authentication challenge.
 */
public enum class AuthChallengeSource {
  @JsonProperty("Server")
  SERVER,
  @JsonProperty("Proxy")
  PROXY,
}
