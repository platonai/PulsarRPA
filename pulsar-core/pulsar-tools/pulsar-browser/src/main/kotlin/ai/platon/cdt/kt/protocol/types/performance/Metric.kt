@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.performance

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.String

/**
 * Run-time execution metric.
 */
data class Metric(
  @param:JsonProperty("name")
  val name: String,
  @param:JsonProperty("value")
  val `value`: Double,
)
