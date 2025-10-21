package ai.platon.cdt.kt.protocol.types.network

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
