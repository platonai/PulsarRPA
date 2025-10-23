@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.debugger

import com.fasterxml.jackson.`annotation`.JsonProperty

public enum class ContinueToLocationTargetCallFrames {
  @JsonProperty("any")
  ANY,
  @JsonProperty("current")
  CURRENT,
}
