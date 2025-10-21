package ai.platon.cdt.kt.protocol.types.overlay

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Configuration data for the highlighting of Flex item elements.
 */
public data class FlexItemHighlightConfig(
  @JsonProperty("baseSizeBox")
  @Optional
  public val baseSizeBox: BoxStyle? = null,
  @JsonProperty("baseSizeBorder")
  @Optional
  public val baseSizeBorder: LineStyle? = null,
  @JsonProperty("flexibilityArrow")
  @Optional
  public val flexibilityArrow: LineStyle? = null,
)
