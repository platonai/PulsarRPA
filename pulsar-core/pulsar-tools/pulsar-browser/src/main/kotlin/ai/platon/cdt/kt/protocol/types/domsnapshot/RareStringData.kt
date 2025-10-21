package ai.platon.cdt.kt.protocol.types.domsnapshot

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int
import kotlin.collections.List

/**
 * Data that is only present on rare nodes.
 */
public data class RareStringData(
  @JsonProperty("index")
  public val index: List<Int>,
  @JsonProperty("value")
  public val `value`: List<Int>,
)
