package ai.platon.cdt.kt.protocol.events.performance

import ai.platon.cdt.kt.protocol.types.performance.Metric
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String
import kotlin.collections.List

/**
 * Current values of the metrics.
 */
public data class Metrics(
  @JsonProperty("metrics")
  public val metrics: List<Metric>,
  @JsonProperty("title")
  public val title: String,
)
