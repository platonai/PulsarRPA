@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.network

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Represents the cookie's 'SameSite' status:
 * https://tools.ietf.org/html/draft-west-first-party-cookies
 */
public enum class CookieSameSite {
  @JsonProperty("Strict")
  STRICT,
  @JsonProperty("Lax")
  LAX,
  @JsonProperty("None")
  NONE,
}
