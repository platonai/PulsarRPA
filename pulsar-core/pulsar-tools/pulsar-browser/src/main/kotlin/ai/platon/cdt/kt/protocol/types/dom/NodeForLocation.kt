package ai.platon.cdt.kt.protocol.types.dom

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int
import kotlin.String

public data class NodeForLocation(
  @JsonProperty("backendNodeId")
  public val backendNodeId: Int,
  @JsonProperty("frameId")
  public val frameId: String,
  @JsonProperty("nodeId")
  @Optional
  public val nodeId: Int? = null,
)
