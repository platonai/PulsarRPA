@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.input

import com.fasterxml.jackson.`annotation`.JsonProperty

public enum class GestureSourceType {
  @JsonProperty("default")
  DEFAULT,
  @JsonProperty("touch")
  TOUCH,
  @JsonProperty("mouse")
  MOUSE,
}
