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
public data class ProfileNode(
  @JsonProperty("id")
  public val id: Int,
  @JsonProperty("callFrame")
  public val callFrame: CallFrame,
  @JsonProperty("hitCount")
  @Optional
  public val hitCount: Int? = null,
  @JsonProperty("children")
  @Optional
  public val children: List<Int>? = null,
  @JsonProperty("deoptReason")
  @Optional
  public val deoptReason: String? = null,
  @JsonProperty("positionTicks")
  @Optional
  public val positionTicks: List<PositionTickInfo>? = null,
)
