@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.heapprofiler

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.Int

/**
 * A single sample from a sampling profile.
 */
data class SamplingHeapProfileSample(
  @param:JsonProperty("size")
  val size: Double,
  @param:JsonProperty("nodeId")
  val nodeId: Int,
  @param:JsonProperty("ordinal")
  val ordinal: Double,
)
