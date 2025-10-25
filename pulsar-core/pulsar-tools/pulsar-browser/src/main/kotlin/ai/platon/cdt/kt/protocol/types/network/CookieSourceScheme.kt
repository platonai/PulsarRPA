@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.network

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Represents the source scheme of the origin that originally set the cookie.
 * A value of "Unset" allows protocol clients to emulate legacy cookie scope for the scheme.
 * This is a temporary ability and it will be removed in the future.
 */
public enum class CookieSourceScheme {
  @JsonProperty("Unset")
  UNSET,
  @JsonProperty("NonSecure")
  NON_SECURE,
  @JsonProperty("Secure")
  SECURE,
}
