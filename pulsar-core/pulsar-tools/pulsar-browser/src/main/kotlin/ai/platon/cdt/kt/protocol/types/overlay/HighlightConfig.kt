@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.overlay

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.types.dom.RGBA
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean

/**
 * Configuration data for the highlighting of page elements.
 */
data class HighlightConfig(
  @param:JsonProperty("showInfo")
  @param:Optional
  val showInfo: Boolean? = null,
  @param:JsonProperty("showStyles")
  @param:Optional
  val showStyles: Boolean? = null,
  @param:JsonProperty("showRulers")
  @param:Optional
  val showRulers: Boolean? = null,
  @param:JsonProperty("showAccessibilityInfo")
  @param:Optional
  val showAccessibilityInfo: Boolean? = null,
  @param:JsonProperty("showExtensionLines")
  @param:Optional
  val showExtensionLines: Boolean? = null,
  @param:JsonProperty("contentColor")
  @param:Optional
  val contentColor: RGBA? = null,
  @param:JsonProperty("paddingColor")
  @param:Optional
  val paddingColor: RGBA? = null,
  @param:JsonProperty("borderColor")
  @param:Optional
  val borderColor: RGBA? = null,
  @param:JsonProperty("marginColor")
  @param:Optional
  val marginColor: RGBA? = null,
  @param:JsonProperty("eventTargetColor")
  @param:Optional
  val eventTargetColor: RGBA? = null,
  @param:JsonProperty("shapeColor")
  @param:Optional
  val shapeColor: RGBA? = null,
  @param:JsonProperty("shapeMarginColor")
  @param:Optional
  val shapeMarginColor: RGBA? = null,
  @param:JsonProperty("cssGridColor")
  @param:Optional
  val cssGridColor: RGBA? = null,
  @param:JsonProperty("colorFormat")
  @param:Optional
  val colorFormat: ColorFormat? = null,
  @param:JsonProperty("gridHighlightConfig")
  @param:Optional
  val gridHighlightConfig: GridHighlightConfig? = null,
  @param:JsonProperty("flexContainerHighlightConfig")
  @param:Optional
  val flexContainerHighlightConfig: FlexContainerHighlightConfig? = null,
  @param:JsonProperty("flexItemHighlightConfig")
  @param:Optional
  val flexItemHighlightConfig: FlexItemHighlightConfig? = null,
  @param:JsonProperty("contrastAlgorithm")
  @param:Optional
  val contrastAlgorithm: ContrastAlgorithm? = null,
)
