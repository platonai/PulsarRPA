@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.dom

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.Int

/**
 * A structure holding an RGBA color.
 */
data class RGBA(
  @param:JsonProperty("r")
  val r: Int,
  @param:JsonProperty("g")
  val g: Int,
  @param:JsonProperty("b")
  val b: Int,
  @param:JsonProperty("a")
  @param:Optional
  val a: Double? = null,
)
