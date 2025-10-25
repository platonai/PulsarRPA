@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.profiler

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int

/**
 * Coverage data for a source range.
 */
data class CoverageRange(
  @param:JsonProperty("startOffset")
  val startOffset: Int,
  @param:JsonProperty("endOffset")
  val endOffset: Int,
  @param:JsonProperty("count")
  val count: Int,
)
