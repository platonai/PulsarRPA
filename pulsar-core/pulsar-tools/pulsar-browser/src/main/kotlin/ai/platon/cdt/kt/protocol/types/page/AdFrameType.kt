package ai.platon.cdt.kt.protocol.types.page

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Indicates whether a frame has been identified as an ad.
 */
public enum class AdFrameType {
  @JsonProperty("none")
  NONE,
  @JsonProperty("child")
  CHILD,
  @JsonProperty("root")
  ROOT,
}
