@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.profiler

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int

/**
 * Specifies a number of samples attributed to a certain source position.
 */
data class PositionTickInfo(
  @param:JsonProperty("line")
  val line: Int,
  @param:JsonProperty("ticks")
  val ticks: Int,
)
