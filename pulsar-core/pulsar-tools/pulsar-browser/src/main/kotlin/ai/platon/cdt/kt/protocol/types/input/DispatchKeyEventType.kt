package ai.platon.cdt.kt.protocol.types.input

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Type of the key event.
 */
public enum class DispatchKeyEventType {
  @JsonProperty("keyDown")
  KEY_DOWN,
  @JsonProperty("keyUp")
  KEY_UP,
  @JsonProperty("rawKeyDown")
  RAW_KEY_DOWN,
  @JsonProperty("char")
  CHAR,
}
