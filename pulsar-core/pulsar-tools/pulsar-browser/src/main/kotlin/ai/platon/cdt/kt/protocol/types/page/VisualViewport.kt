@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.page

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double

/**
 * Visual viewport position, dimensions, and scale.
 */
data class VisualViewport(
  @param:JsonProperty("offsetX")
  val offsetX: Double,
  @param:JsonProperty("offsetY")
  val offsetY: Double,
  @param:JsonProperty("pageX")
  val pageX: Double,
  @param:JsonProperty("pageY")
  val pageY: Double,
  @param:JsonProperty("clientWidth")
  val clientWidth: Double,
  @param:JsonProperty("clientHeight")
  val clientHeight: Double,
  @param:JsonProperty("scale")
  val scale: Double,
  @param:JsonProperty("zoom")
  @param:Optional
  val zoom: Double? = null,
)
