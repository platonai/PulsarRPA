@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.profiler

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.collections.List

data class TakePreciseCoverage(
  @param:JsonProperty("result")
  val result: List<ScriptCoverage>,
  @param:JsonProperty("timestamp")
  val timestamp: Double,
)
