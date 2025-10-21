package ai.platon.cdt.kt.protocol.types.performance

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.String

/**
 * Run-time execution metric.
 */
public data class Metric(
  @JsonProperty("name")
  public val name: String,
  @JsonProperty("value")
  public val `value`: Double,
)
