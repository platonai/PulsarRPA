package ai.platon.cdt.kt.protocol.events.dom

import ai.platon.cdt.kt.protocol.types.dom.Node
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int

/**
 * Mirrors `DOMNodeInserted` event.
 */
public data class ChildNodeInserted(
  @JsonProperty("parentNodeId")
  public val parentNodeId: Int,
  @JsonProperty("previousNodeId")
  public val previousNodeId: Int,
  @JsonProperty("node")
  public val node: Node,
)
