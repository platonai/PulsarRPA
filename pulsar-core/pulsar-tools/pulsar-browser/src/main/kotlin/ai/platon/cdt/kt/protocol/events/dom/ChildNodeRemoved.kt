@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.dom

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int

/**
 * Mirrors `DOMNodeRemoved` event.
 */
data class ChildNodeRemoved(
  @param:JsonProperty("parentNodeId")
  val parentNodeId: Int,
  @param:JsonProperty("nodeId")
  val nodeId: Int,
)
