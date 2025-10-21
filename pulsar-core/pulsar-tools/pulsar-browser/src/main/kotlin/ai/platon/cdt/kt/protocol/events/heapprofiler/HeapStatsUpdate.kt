package ai.platon.cdt.kt.protocol.events.heapprofiler

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int
import kotlin.collections.List

/**
 * If heap objects tracking has been started then backend may send update for one or more fragments
 */
public data class HeapStatsUpdate(
  @JsonProperty("statsUpdate")
  public val statsUpdate: List<Int>,
)
