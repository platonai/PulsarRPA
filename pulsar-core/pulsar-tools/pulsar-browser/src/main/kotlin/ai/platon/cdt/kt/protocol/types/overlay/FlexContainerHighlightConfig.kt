@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.overlay

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Configuration data for the highlighting of Flex container elements.
 */
data class FlexContainerHighlightConfig(
  @param:JsonProperty("containerBorder")
  @param:Optional
  val containerBorder: LineStyle? = null,
  @param:JsonProperty("lineSeparator")
  @param:Optional
  val lineSeparator: LineStyle? = null,
  @param:JsonProperty("itemSeparator")
  @param:Optional
  val itemSeparator: LineStyle? = null,
  @param:JsonProperty("mainDistributedSpace")
  @param:Optional
  val mainDistributedSpace: BoxStyle? = null,
  @param:JsonProperty("crossDistributedSpace")
  @param:Optional
  val crossDistributedSpace: BoxStyle? = null,
  @param:JsonProperty("rowGapSpace")
  @param:Optional
  val rowGapSpace: BoxStyle? = null,
  @param:JsonProperty("columnGapSpace")
  @param:Optional
  val columnGapSpace: BoxStyle? = null,
  @param:JsonProperty("crossAlignment")
  @param:Optional
  val crossAlignment: LineStyle? = null,
)
