@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.audits

import com.fasterxml.jackson.`annotation`.JsonProperty

public enum class HeavyAdResolutionStatus {
  @JsonProperty("HeavyAdBlocked")
  HEAVY_AD_BLOCKED,
  @JsonProperty("HeavyAdWarning")
  HEAVY_AD_WARNING,
}
