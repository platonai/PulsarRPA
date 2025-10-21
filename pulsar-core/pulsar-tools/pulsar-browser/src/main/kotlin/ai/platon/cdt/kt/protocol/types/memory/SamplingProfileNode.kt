package ai.platon.cdt.kt.protocol.types.memory

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.String
import kotlin.collections.List

/**
 * Heap profile sample.
 */
public data class SamplingProfileNode(
  @JsonProperty("size")
  public val size: Double,
  @JsonProperty("total")
  public val total: Double,
  @JsonProperty("stack")
  public val stack: List<String>,
)
