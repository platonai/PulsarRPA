@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.heapprofiler

import ai.platon.cdt.kt.protocol.types.runtime.CallFrame
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.Int
import kotlin.collections.List

/**
 * Sampling Heap Profile node. Holds callsite information, allocation statistics and child nodes.
 */
data class SamplingHeapProfileNode(
  @param:JsonProperty("callFrame")
  val callFrame: CallFrame,
  @param:JsonProperty("selfSize")
  val selfSize: Double,
  @param:JsonProperty("id")
  val id: Int,
  @param:JsonProperty("children")
  val children: List<SamplingHeapProfileNode>,
)
