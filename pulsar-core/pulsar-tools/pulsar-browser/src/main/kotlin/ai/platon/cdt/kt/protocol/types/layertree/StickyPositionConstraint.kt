package ai.platon.cdt.kt.protocol.types.layertree

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.types.dom.Rect
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * Sticky position constraints.
 */
public data class StickyPositionConstraint(
  @JsonProperty("stickyBoxRect")
  public val stickyBoxRect: Rect,
  @JsonProperty("containingBlockRect")
  public val containingBlockRect: Rect,
  @JsonProperty("nearestLayerShiftingStickyBox")
  @Optional
  public val nearestLayerShiftingStickyBox: String? = null,
  @JsonProperty("nearestLayerShiftingContainingBlock")
  @Optional
  public val nearestLayerShiftingContainingBlock: String? = null,
)
