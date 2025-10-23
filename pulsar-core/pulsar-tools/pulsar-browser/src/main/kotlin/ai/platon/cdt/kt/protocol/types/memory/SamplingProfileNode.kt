@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.memory

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.String
import kotlin.collections.List

/**
 * Heap profile sample.
 */
data class SamplingProfileNode(
  @param:JsonProperty("size")
  val size: Double,
  @param:JsonProperty("total")
  val total: Double,
  @param:JsonProperty("stack")
  val stack: List<String>,
)
