@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.profiler

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.String
import kotlin.collections.List

/**
 * Coverage data for a JavaScript function.
 */
data class FunctionCoverage(
  @param:JsonProperty("functionName")
  val functionName: String,
  @param:JsonProperty("ranges")
  val ranges: List<CoverageRange>,
  @param:JsonProperty("isBlockCoverage")
  val isBlockCoverage: Boolean,
)
