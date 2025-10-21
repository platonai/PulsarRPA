package ai.platon.cdt.kt.protocol.types.profiler

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int

/**
 * Coverage data for a source range.
 */
public data class CoverageRange(
  @JsonProperty("startOffset")
  public val startOffset: Int,
  @JsonProperty("endOffset")
  public val endOffset: Int,
  @JsonProperty("count")
  public val count: Int,
)
