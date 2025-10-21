package ai.platon.cdt.kt.protocol.types.input

import com.fasterxml.jackson.`annotation`.JsonProperty

public enum class MouseButton {
  @JsonProperty("none")
  NONE,
  @JsonProperty("left")
  LEFT,
  @JsonProperty("middle")
  MIDDLE,
  @JsonProperty("right")
  RIGHT,
  @JsonProperty("back")
  BACK,
  @JsonProperty("forward")
  FORWARD,
}
