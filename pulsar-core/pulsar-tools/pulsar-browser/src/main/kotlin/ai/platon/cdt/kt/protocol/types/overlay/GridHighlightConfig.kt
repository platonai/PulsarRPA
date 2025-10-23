@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.overlay

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.types.dom.RGBA
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.Deprecated

/**
 * Configuration data for the highlighting of Grid elements.
 */
data class GridHighlightConfig(
  @param:JsonProperty("showGridExtensionLines")
  @param:Optional
  val showGridExtensionLines: Boolean? = null,
  @param:JsonProperty("showPositiveLineNumbers")
  @param:Optional
  val showPositiveLineNumbers: Boolean? = null,
  @param:JsonProperty("showNegativeLineNumbers")
  @param:Optional
  val showNegativeLineNumbers: Boolean? = null,
  @param:JsonProperty("showAreaNames")
  @param:Optional
  val showAreaNames: Boolean? = null,
  @param:JsonProperty("showLineNames")
  @param:Optional
  val showLineNames: Boolean? = null,
  @param:JsonProperty("showTrackSizes")
  @param:Optional
  val showTrackSizes: Boolean? = null,
  @param:JsonProperty("gridBorderColor")
  @param:Optional
  val gridBorderColor: RGBA? = null,
  @param:JsonProperty("cellBorderColor")
  @param:Optional
  @Deprecated("Deprecated by protocol")
  val cellBorderColor: RGBA? = null,
  @param:JsonProperty("rowLineColor")
  @param:Optional
  val rowLineColor: RGBA? = null,
  @param:JsonProperty("columnLineColor")
  @param:Optional
  val columnLineColor: RGBA? = null,
  @param:JsonProperty("gridBorderDash")
  @param:Optional
  val gridBorderDash: Boolean? = null,
  @param:JsonProperty("cellBorderDash")
  @param:Optional
  @Deprecated("Deprecated by protocol")
  val cellBorderDash: Boolean? = null,
  @param:JsonProperty("rowLineDash")
  @param:Optional
  val rowLineDash: Boolean? = null,
  @param:JsonProperty("columnLineDash")
  @param:Optional
  val columnLineDash: Boolean? = null,
  @param:JsonProperty("rowGapColor")
  @param:Optional
  val rowGapColor: RGBA? = null,
  @param:JsonProperty("rowHatchColor")
  @param:Optional
  val rowHatchColor: RGBA? = null,
  @param:JsonProperty("columnGapColor")
  @param:Optional
  val columnGapColor: RGBA? = null,
  @param:JsonProperty("columnHatchColor")
  @param:Optional
  val columnHatchColor: RGBA? = null,
  @param:JsonProperty("areaBorderColor")
  @param:Optional
  val areaBorderColor: RGBA? = null,
  @param:JsonProperty("gridBackgroundColor")
  @param:Optional
  val gridBackgroundColor: RGBA? = null,
)
