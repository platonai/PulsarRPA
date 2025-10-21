package ai.platon.cdt.kt.protocol.types.emulation

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Orientation of a display feature in relation to screen
 */
public enum class DisplayFeatureOrientation {
  @JsonProperty("vertical")
  VERTICAL,
  @JsonProperty("horizontal")
  HORIZONTAL,
}
