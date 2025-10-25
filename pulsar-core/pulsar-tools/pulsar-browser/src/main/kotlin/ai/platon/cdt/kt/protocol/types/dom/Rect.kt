@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.dom

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double

/**
 * Rectangle.
 */
data class Rect(
  @param:JsonProperty("x")
  val x: Double,
  @param:JsonProperty("y")
  val y: Double,
  @param:JsonProperty("width")
  val width: Double,
  @param:JsonProperty("height")
  val height: Double,
)
