package ai.platon.cdt.kt.protocol.types.network

import com.fasterxml.jackson.`annotation`.JsonProperty

public enum class CrossOriginOpenerPolicyValue {
  @JsonProperty("SameOrigin")
  SAME_ORIGIN,
  @JsonProperty("SameOriginAllowPopups")
  SAME_ORIGIN_ALLOW_POPUPS,
  @JsonProperty("UnsafeNone")
  UNSAFE_NONE,
  @JsonProperty("SameOriginPlusCoep")
  SAME_ORIGIN_PLUS_COEP,
}
