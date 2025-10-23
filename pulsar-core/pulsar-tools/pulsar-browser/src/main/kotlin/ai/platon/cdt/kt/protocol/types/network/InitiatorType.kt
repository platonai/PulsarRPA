@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.network

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Type of this initiator.
 */
public enum class InitiatorType {
  @JsonProperty("parser")
  PARSER,
  @JsonProperty("script")
  SCRIPT,
  @JsonProperty("preload")
  PRELOAD,
  @JsonProperty("SignedExchange")
  SIGNED_EXCHANGE,
  @JsonProperty("preflight")
  PREFLIGHT,
  @JsonProperty("other")
  OTHER,
}
