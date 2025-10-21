package ai.platon.cdt.kt.protocol.types.memory

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int

public data class DOMCounters(
  @JsonProperty("documents")
  public val documents: Int,
  @JsonProperty("nodes")
  public val nodes: Int,
  @JsonProperty("jsEventListeners")
  public val jsEventListeners: Int,
)
