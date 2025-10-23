@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.dom

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Any
import kotlin.Double
import kotlin.collections.List

/**
 * CSS Shape Outside details.
 */
data class ShapeOutsideInfo(
  @param:JsonProperty("bounds")
  val bounds: List<Double>,
  @param:JsonProperty("shape")
  val shape: List<Any?>,
  @param:JsonProperty("marginShape")
  val marginShape: List<Any?>,
)
