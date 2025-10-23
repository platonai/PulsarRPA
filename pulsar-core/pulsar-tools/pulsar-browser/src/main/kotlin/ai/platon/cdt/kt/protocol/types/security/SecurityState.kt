@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.security

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * The security level of a page or resource.
 */
public enum class SecurityState {
  @JsonProperty("unknown")
  UNKNOWN,
  @JsonProperty("neutral")
  NEUTRAL,
  @JsonProperty("insecure")
  INSECURE,
  @JsonProperty("secure")
  SECURE,
  @JsonProperty("info")
  INFO,
  @JsonProperty("insecure-broken")
  INSECURE_BROKEN,
}
