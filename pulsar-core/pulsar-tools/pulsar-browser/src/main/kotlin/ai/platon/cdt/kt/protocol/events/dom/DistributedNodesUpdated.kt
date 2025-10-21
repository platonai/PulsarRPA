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
public data class DistributedNodesUpdated(
  @JsonProperty("insertionPointId")
  public val insertionPointId: Int,
  @JsonProperty("distributedNodes")
  public val distributedNodes: List<BackendNode>,
)
