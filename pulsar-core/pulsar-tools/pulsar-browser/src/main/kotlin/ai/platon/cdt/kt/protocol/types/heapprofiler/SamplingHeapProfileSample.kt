package ai.platon.cdt.kt.protocol.types.heapprofiler

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.Int

/**
 * A single sample from a sampling profile.
 */
public data class SamplingHeapProfileSample(
  @JsonProperty("size")
  public val size: Double,
  @JsonProperty("nodeId")
  public val nodeId: Int,
  @JsonProperty("ordinal")
  public val ordinal: Double,
)
