package ai.platon.cdt.kt.protocol.types.dom

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int

public data class FrameOwner(
  @JsonProperty("backendNodeId")
  public val backendNodeId: Int,
  @JsonProperty("nodeId")
  @Optional
  public val nodeId: Int? = null,
)
