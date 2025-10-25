@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.layertree

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.String

/**
 * Serialized fragment of layer picture along with its offset within the layer.
 */
data class PictureTile(
  @param:JsonProperty("x")
  val x: Double,
  @param:JsonProperty("y")
  val y: Double,
  @param:JsonProperty("picture")
  val picture: String,
)
