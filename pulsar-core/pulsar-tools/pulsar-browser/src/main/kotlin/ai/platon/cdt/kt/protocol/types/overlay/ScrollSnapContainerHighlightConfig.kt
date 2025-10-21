package ai.platon.cdt.kt.protocol.types.overlay

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.types.dom.RGBA
import com.fasterxml.jackson.`annotation`.JsonProperty

public data class ScrollSnapContainerHighlightConfig(
  @JsonProperty("snapportBorder")
  @Optional
  public val snapportBorder: LineStyle? = null,
  @JsonProperty("snapAreaBorder")
  @Optional
  public val snapAreaBorder: LineStyle? = null,
  @JsonProperty("scrollMarginColor")
  @Optional
  public val scrollMarginColor: RGBA? = null,
  @JsonProperty("scrollPaddingColor")
  @Optional
  public val scrollPaddingColor: RGBA? = null,
)
