package ai.platon.cdt.kt.protocol.events.dom

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int

/**
 * Mirrors `DOMNodeRemoved` event.
 */
public data class ChildNodeRemoved(
  @JsonProperty("parentNodeId")
  public val parentNodeId: Int,
  @JsonProperty("nodeId")
  public val nodeId: Int,
)
