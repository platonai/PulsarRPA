@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.css

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.collections.List

data class TakeCoverageDelta(
  @param:JsonProperty("coverage")
  val coverage: List<RuleUsage>,
  @param:JsonProperty("timestamp")
  val timestamp: Double,
)
