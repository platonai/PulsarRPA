package ai.platon.cdt.kt.protocol.types.layertree

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Reason for rectangle to force scrolling on the main thread
 */
public enum class ScrollRectType {
  @JsonProperty("RepaintsOnScroll")
  REPAINTS_ON_SCROLL,
  @JsonProperty("TouchEventHandler")
  TOUCH_EVENT_HANDLER,
  @JsonProperty("WheelEventHandler")
  WHEEL_EVENT_HANDLER,
}
