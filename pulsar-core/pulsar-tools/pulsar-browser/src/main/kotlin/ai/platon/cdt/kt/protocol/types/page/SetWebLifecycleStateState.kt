package ai.platon.cdt.kt.protocol.types.page

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Target lifecycle state
 */
public enum class SetWebLifecycleStateState {
  @JsonProperty("frozen")
  FROZEN,
  @JsonProperty("active")
  ACTIVE,
}
