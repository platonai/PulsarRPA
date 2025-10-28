@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.network

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue
import com.fasterxml.jackson.`annotation`.JsonProperty

public enum class PrivateNetworkRequestPolicy {
  @JsonProperty("Allow")
  ALLOW,
  @JsonProperty("BlockFromInsecureToMorePrivate")
  BLOCK_FROM_INSECURE_TO_MORE_PRIVATE,
  @JsonProperty("WarnFromInsecureToMorePrivate")
  WARN_FROM_INSECURE_TO_MORE_PRIVATE,
    // vincent: 20251028
    // Default fallback for any unknown name
    @JsonEnumDefaultValue
    UNKNOWN,
}
