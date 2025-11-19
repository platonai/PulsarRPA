@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.dom

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.Int
import kotlin.collections.List

/**
 * Box model.
 */
data class BoxModel(
  @param:JsonProperty("content")
  val content: List<Double>,
  @param:JsonProperty("padding")
  val padding: List<Double>,
  @param:JsonProperty("border")
  val border: List<Double>,
  @param:JsonProperty("margin")
  val margin: List<Double>,
  @param:JsonProperty("width")
  val width: Int,
  @param:JsonProperty("height")
  val height: Int,
  @param:JsonProperty("shapeOutside")
  @param:Optional
  val shapeOutside: ShapeOutsideInfo? = null,
)
