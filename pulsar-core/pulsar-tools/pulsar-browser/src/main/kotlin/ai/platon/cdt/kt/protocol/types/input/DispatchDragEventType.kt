@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.input

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Type of the drag event.
 */
public enum class DispatchDragEventType {
  @JsonProperty("dragEnter")
  DRAG_ENTER,
  @JsonProperty("dragOver")
  DRAG_OVER,
  @JsonProperty("drop")
  DROP,
  @JsonProperty("dragCancel")
  DRAG_CANCEL,
}
