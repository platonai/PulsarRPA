@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.performance

import ai.platon.cdt.kt.protocol.types.performance.Metric
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String
import kotlin.collections.List

/**
 * Current values of the metrics.
 */
data class Metrics(
  @param:JsonProperty("metrics")
  val metrics: List<Metric>,
  @param:JsonProperty("title")
  val title: String,
)
