@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.css

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.String

/**
 * Information about font variation axes for variable fonts
 */
data class FontVariationAxis(
  @param:JsonProperty("tag")
  val tag: String,
  @param:JsonProperty("name")
  val name: String,
  @param:JsonProperty("minValue")
  val minValue: Double,
  @param:JsonProperty("maxValue")
  val maxValue: Double,
  @param:JsonProperty("defaultValue")
  val defaultValue: Double,
)
