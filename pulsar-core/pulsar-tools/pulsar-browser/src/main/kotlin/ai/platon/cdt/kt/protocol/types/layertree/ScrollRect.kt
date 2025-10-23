@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.layertree

import ai.platon.cdt.kt.protocol.types.dom.Rect
import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Rectangle where scrolling happens on the main thread.
 */
data class ScrollRect(
  @param:JsonProperty("rect")
  val rect: Rect,
  @param:JsonProperty("type")
  val type: ScrollRectType,
)
