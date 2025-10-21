package ai.platon.cdt.kt.protocol.types.input

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Type of the touch event. TouchEnd and TouchCancel must not contain any touch points, while
 * TouchStart and TouchMove must contains at least one.
 */
public enum class DispatchTouchEventType {
  @JsonProperty("touchStart")
  TOUCH_START,
  @JsonProperty("touchEnd")
  TOUCH_END,
  @JsonProperty("touchMove")
  TOUCH_MOVE,
  @JsonProperty("touchCancel")
  TOUCH_CANCEL,
}
