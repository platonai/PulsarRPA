@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.input

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Pointer type (default: "mouse").
 */
public enum class DispatchMouseEventPointerType {
  @JsonProperty("mouse")
  MOUSE,
  @JsonProperty("pen")
  PEN,
}
