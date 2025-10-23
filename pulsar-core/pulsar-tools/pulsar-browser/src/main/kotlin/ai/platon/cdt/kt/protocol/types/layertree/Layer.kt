@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.layertree

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.Double
import kotlin.Int
import kotlin.String
import kotlin.collections.List

/**
 * Information about a compositing layer.
 */
data class Layer(
  @param:JsonProperty("layerId")
  val layerId: String,
  @param:JsonProperty("parentLayerId")
  @param:Optional
  val parentLayerId: String? = null,
  @param:JsonProperty("backendNodeId")
  @param:Optional
  val backendNodeId: Int? = null,
  @param:JsonProperty("offsetX")
  val offsetX: Double,
  @param:JsonProperty("offsetY")
  val offsetY: Double,
  @param:JsonProperty("width")
  val width: Double,
  @param:JsonProperty("height")
  val height: Double,
  @param:JsonProperty("transform")
  @param:Optional
  val transform: List<Double>? = null,
  @param:JsonProperty("anchorX")
  @param:Optional
  val anchorX: Double? = null,
  @param:JsonProperty("anchorY")
  @param:Optional
  val anchorY: Double? = null,
  @param:JsonProperty("anchorZ")
  @param:Optional
  val anchorZ: Double? = null,
  @param:JsonProperty("paintCount")
  val paintCount: Int,
  @param:JsonProperty("drawsContent")
  val drawsContent: Boolean,
  @param:JsonProperty("invisible")
  @param:Optional
  val invisible: Boolean? = null,
  @param:JsonProperty("scrollRects")
  @param:Optional
  val scrollRects: List<ScrollRect>? = null,
  @param:JsonProperty("stickyPositionConstraint")
  @param:Optional
  val stickyPositionConstraint: StickyPositionConstraint? = null,
)
