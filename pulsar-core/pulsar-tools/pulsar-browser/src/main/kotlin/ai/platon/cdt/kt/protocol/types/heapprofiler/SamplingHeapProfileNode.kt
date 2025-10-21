package ai.platon.cdt.kt.protocol.types.heapprofiler

import ai.platon.cdt.kt.protocol.types.runtime.CallFrame
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.Int
import kotlin.collections.List

/**
 * Sampling Heap Profile node. Holds callsite information, allocation statistics and child nodes.
 */
public data class SamplingHeapProfileNode(
  @JsonProperty("callFrame")
  public val callFrame: CallFrame,
  @JsonProperty("selfSize")
  public val selfSize: Double,
  @JsonProperty("id")
  public val id: Int,
  @JsonProperty("children")
  public val children: List<SamplingHeapProfileNode>,
)
