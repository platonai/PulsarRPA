package ai.platon.cdt.kt.protocol.types.overlay

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.types.dom.RGBA
import com.fasterxml.jackson.`annotation`.JsonProperty
import java.lang.Deprecated
import kotlin.Boolean

/**
 * Configuration data for the highlighting of Grid elements.
 */
public data class GridHighlightConfig(
  @JsonProperty("showGridExtensionLines")
  @Optional
  public val showGridExtensionLines: Boolean? = null,
  @JsonProperty("showPositiveLineNumbers")
  @Optional
  public val showPositiveLineNumbers: Boolean? = null,
  @JsonProperty("showNegativeLineNumbers")
  @Optional
  public val showNegativeLineNumbers: Boolean? = null,
  @JsonProperty("showAreaNames")
  @Optional
  public val showAreaNames: Boolean? = null,
  @JsonProperty("showLineNames")
  @Optional
  public val showLineNames: Boolean? = null,
  @JsonProperty("showTrackSizes")
  @Optional
  public val showTrackSizes: Boolean? = null,
  @JsonProperty("gridBorderColor")
  @Optional
  public val gridBorderColor: RGBA? = null,
  @JsonProperty("cellBorderColor")
  @Optional
  @Deprecated
  public val cellBorderColor: RGBA? = null,
  @JsonProperty("rowLineColor")
  @Optional
  public val rowLineColor: RGBA? = null,
  @JsonProperty("columnLineColor")
  @Optional
  public val columnLineColor: RGBA? = null,
  @JsonProperty("gridBorderDash")
  @Optional
  public val gridBorderDash: Boolean? = null,
  @JsonProperty("cellBorderDash")
  @Optional
  @Deprecated
  public val cellBorderDash: Boolean? = null,
  @JsonProperty("rowLineDash")
  @Optional
  public val rowLineDash: Boolean? = null,
  @JsonProperty("columnLineDash")
  @Optional
  public val columnLineDash: Boolean? = null,
  @JsonProperty("rowGapColor")
  @Optional
  public val rowGapColor: RGBA? = null,
  @JsonProperty("rowHatchColor")
  @Optional
  public val rowHatchColor: RGBA? = null,
  @JsonProperty("columnGapColor")
  @Optional
  public val columnGapColor: RGBA? = null,
  @JsonProperty("columnHatchColor")
  @Optional
  public val columnHatchColor: RGBA? = null,
  @JsonProperty("areaBorderColor")
  @Optional
  public val areaBorderColor: RGBA? = null,
  @JsonProperty("gridBackgroundColor")
  @Optional
  public val gridBackgroundColor: RGBA? = null,
)
