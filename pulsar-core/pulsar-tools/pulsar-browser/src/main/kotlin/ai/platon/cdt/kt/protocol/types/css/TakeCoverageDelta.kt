package ai.platon.cdt.kt.protocol.types.css

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.collections.List

public data class TakeCoverageDelta(
  @JsonProperty("coverage")
  public val coverage: List<RuleUsage>,
  @JsonProperty("timestamp")
  public val timestamp: Double,
)
