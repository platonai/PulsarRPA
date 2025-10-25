@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.network

import com.fasterxml.jackson.`annotation`.JsonProperty

public enum class IPAddressSpace {
  @JsonProperty("Local")
  LOCAL,
  @JsonProperty("Private")
  PRIVATE,
  @JsonProperty("Public")
  PUBLIC,
  @JsonProperty("Unknown")
  UNKNOWN,
}
