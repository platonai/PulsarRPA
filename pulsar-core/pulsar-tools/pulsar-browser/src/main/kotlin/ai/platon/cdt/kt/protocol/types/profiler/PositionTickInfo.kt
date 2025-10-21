package ai.platon.cdt.kt.protocol.types.profiler

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int

/**
 * Specifies a number of samples attributed to a certain source position.
 */
public data class PositionTickInfo(
  @JsonProperty("line")
  public val line: Int,
  @JsonProperty("ticks")
  public val ticks: Int,
)
