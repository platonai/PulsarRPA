@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.page

import ai.platon.cdt.kt.protocol.types.dom.Rect
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Deprecated

data class LayoutMetrics(
  @param:JsonProperty("layoutViewport")
  @Deprecated("Deprecated by protocol")
  val layoutViewport: LayoutViewport,
  @param:JsonProperty("visualViewport")
  @Deprecated("Deprecated by protocol")
  val visualViewport: VisualViewport,
  @param:JsonProperty("contentSize")
  @Deprecated("Deprecated by protocol")
  val contentSize: Rect,
  @param:JsonProperty("cssLayoutViewport")
  val cssLayoutViewport: LayoutViewport,
  @param:JsonProperty("cssVisualViewport")
  val cssVisualViewport: VisualViewport,
  @param:JsonProperty("cssContentSize")
  val cssContentSize: Rect,
)
