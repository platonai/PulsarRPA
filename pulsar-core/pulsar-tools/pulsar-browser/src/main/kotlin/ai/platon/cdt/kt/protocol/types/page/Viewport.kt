@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.page

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double

/**
 * Viewport for capturing screenshot.
 */
data class Viewport(
  @param:JsonProperty("x")
  val x: Double,
  @param:JsonProperty("y")
  val y: Double,
  @param:JsonProperty("width")
  val width: Double,
  @param:JsonProperty("height")
  val height: Double,
  @param:JsonProperty("scale")
  val scale: Double,
)
