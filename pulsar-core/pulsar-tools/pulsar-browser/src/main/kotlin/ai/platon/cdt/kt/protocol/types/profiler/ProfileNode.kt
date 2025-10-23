@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.profiler

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.types.runtime.CallFrame
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int
import kotlin.String
import kotlin.collections.List

/**
 * Profile node. Holds callsite information, execution statistics and child nodes.
 */
data class ProfileNode(
  @param:JsonProperty("id")
  val id: Int,
  @param:JsonProperty("callFrame")
  val callFrame: CallFrame,
  @param:JsonProperty("hitCount")
  @param:Optional
  val hitCount: Int? = null,
  @param:JsonProperty("children")
  @param:Optional
  val children: List<Int>? = null,
  @param:JsonProperty("deoptReason")
  @param:Optional
  val deoptReason: String? = null,
  @param:JsonProperty("positionTicks")
  @param:Optional
  val positionTicks: List<PositionTickInfo>? = null,
)
