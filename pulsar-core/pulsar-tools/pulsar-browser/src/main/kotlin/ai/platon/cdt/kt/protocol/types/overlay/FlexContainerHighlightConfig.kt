package ai.platon.cdt.kt.protocol.types.overlay

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Configuration data for the highlighting of Flex container elements.
 */
public data class FlexContainerHighlightConfig(
  @JsonProperty("containerBorder")
  @Optional
  public val containerBorder: LineStyle? = null,
  @JsonProperty("lineSeparator")
  @Optional
  public val lineSeparator: LineStyle? = null,
  @JsonProperty("itemSeparator")
  @Optional
  public val itemSeparator: LineStyle? = null,
  @JsonProperty("mainDistributedSpace")
  @Optional
  public val mainDistributedSpace: BoxStyle? = null,
  @JsonProperty("crossDistributedSpace")
  @Optional
  public val crossDistributedSpace: BoxStyle? = null,
  @JsonProperty("rowGapSpace")
  @Optional
  public val rowGapSpace: BoxStyle? = null,
  @JsonProperty("columnGapSpace")
  @Optional
  public val columnGapSpace: BoxStyle? = null,
  @JsonProperty("crossAlignment")
  @Optional
  public val crossAlignment: LineStyle? = null,
)
