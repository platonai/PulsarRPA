@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.systeminfo

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int

/**
 * Describes the width and height dimensions of an entity.
 */
data class Size(
  @param:JsonProperty("width")
  val width: Int,
  @param:JsonProperty("height")
  val height: Int,
)
