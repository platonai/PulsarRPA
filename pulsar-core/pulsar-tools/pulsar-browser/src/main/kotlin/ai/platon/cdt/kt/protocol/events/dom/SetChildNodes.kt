package ai.platon.cdt.kt.protocol.events.dom

import ai.platon.cdt.kt.protocol.types.dom.Node
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int
import kotlin.collections.List

/**
 * Fired when backend wants to provide client with the missing DOM structure. This happens upon
 * most of the calls requesting node ids.
 */
public data class SetChildNodes(
  @JsonProperty("parentId")
  public val parentId: Int,
  @JsonProperty("nodes")
  public val nodes: List<Node>,
)
