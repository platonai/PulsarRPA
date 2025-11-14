@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.dom

import ai.platon.cdt.kt.protocol.types.dom.Node
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int

/**
 * Mirrors `DOMNodeInserted` event.
 */
data class ChildNodeInserted(
  @param:JsonProperty("parentNodeId")
  val parentNodeId: Int,
  @param:JsonProperty("previousNodeId")
  val previousNodeId: Int,
  @param:JsonProperty("node")
  val node: Node,
)
