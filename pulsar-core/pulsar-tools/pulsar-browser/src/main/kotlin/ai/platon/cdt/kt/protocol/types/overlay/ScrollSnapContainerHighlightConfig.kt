@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.overlay

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.types.dom.RGBA
import com.fasterxml.jackson.`annotation`.JsonProperty

data class ScrollSnapContainerHighlightConfig(
  @param:JsonProperty("snapportBorder")
  @param:Optional
  val snapportBorder: LineStyle? = null,
  @param:JsonProperty("snapAreaBorder")
  @param:Optional
  val snapAreaBorder: LineStyle? = null,
  @param:JsonProperty("scrollMarginColor")
  @param:Optional
  val scrollMarginColor: RGBA? = null,
  @param:JsonProperty("scrollPaddingColor")
  @param:Optional
  val scrollPaddingColor: RGBA? = null,
)
