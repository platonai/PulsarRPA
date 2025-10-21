package ai.platon.cdt.kt.protocol.types.page

import com.fasterxml.jackson.`annotation`.JsonProperty

public enum class FrameDetachedReason {
  @JsonProperty("remove")
  REMOVE,
  @JsonProperty("swap")
  SWAP,
}
