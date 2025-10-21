package ai.platon.cdt.kt.protocol.types.profiler

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.Int
import kotlin.collections.List

/**
 * Profile.
 */
public data class Profile(
  @JsonProperty("nodes")
  public val nodes: List<ProfileNode>,
  @JsonProperty("startTime")
  public val startTime: Double,
  @JsonProperty("endTime")
  public val endTime: Double,
  @JsonProperty("samples")
  @Optional
  public val samples: List<Int>? = null,
  @JsonProperty("timeDeltas")
  @Optional
  public val timeDeltas: List<Int>? = null,
)
