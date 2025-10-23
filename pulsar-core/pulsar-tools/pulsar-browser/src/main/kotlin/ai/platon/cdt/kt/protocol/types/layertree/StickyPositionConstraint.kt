@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.layertree

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.types.dom.Rect
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * Sticky position constraints.
 */
data class StickyPositionConstraint(
  @param:JsonProperty("stickyBoxRect")
  val stickyBoxRect: Rect,
  @param:JsonProperty("containingBlockRect")
  val containingBlockRect: Rect,
  @param:JsonProperty("nearestLayerShiftingStickyBox")
  @param:Optional
  val nearestLayerShiftingStickyBox: String? = null,
  @param:JsonProperty("nearestLayerShiftingContainingBlock")
  @param:Optional
  val nearestLayerShiftingContainingBlock: String? = null,
)
