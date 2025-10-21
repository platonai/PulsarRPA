package ai.platon.cdt.kt.protocol.types.layertree

import ai.platon.cdt.kt.protocol.types.dom.Rect
import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Rectangle where scrolling happens on the main thread.
 */
public data class ScrollRect(
  @JsonProperty("rect")
  public val rect: Rect,
  @JsonProperty("type")
  public val type: ScrollRectType,
)
