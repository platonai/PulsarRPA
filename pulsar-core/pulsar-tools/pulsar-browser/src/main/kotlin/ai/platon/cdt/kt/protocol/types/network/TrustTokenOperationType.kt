package ai.platon.cdt.kt.protocol.types.network

import com.fasterxml.jackson.`annotation`.JsonProperty

public enum class TrustTokenOperationType {
  @JsonProperty("Issuance")
  ISSUANCE,
  @JsonProperty("Redemption")
  REDEMPTION,
  @JsonProperty("Signing")
  SIGNING,
}
