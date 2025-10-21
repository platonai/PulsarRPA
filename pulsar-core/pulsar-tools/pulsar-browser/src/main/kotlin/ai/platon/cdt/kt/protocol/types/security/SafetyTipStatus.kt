package ai.platon.cdt.kt.protocol.types.security

import com.fasterxml.jackson.`annotation`.JsonProperty

public enum class SafetyTipStatus {
  @JsonProperty("badReputation")
  BAD_REPUTATION,
  @JsonProperty("lookalike")
  LOOKALIKE,
}
