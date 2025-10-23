@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.heapprofiler

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.collections.List

/**
 * Sampling profile.
 */
data class SamplingHeapProfile(
  @param:JsonProperty("head")
  val head: SamplingHeapProfileNode,
  @param:JsonProperty("samples")
  val samples: List<SamplingHeapProfileSample>,
)
