@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.dom

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.types.dom.BackendNode
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int
import kotlin.collections.List

/**
 * Called when distribution is changed.
 */
@Experimental
data class DistributedNodesUpdated(
  @param:JsonProperty("insertionPointId")
  val insertionPointId: Int,
  @param:JsonProperty("distributedNodes")
  val distributedNodes: List<BackendNode>,
)
