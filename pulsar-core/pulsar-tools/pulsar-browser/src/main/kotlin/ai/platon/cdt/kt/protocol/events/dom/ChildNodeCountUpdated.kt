package ai.platon.cdt.kt.protocol.events.dom

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int

/**
 * Fired when `Container`'s child node count has changed.
 */
public data class ChildNodeCountUpdated(
  @JsonProperty("nodeId")
  public val nodeId: Int,
  @JsonProperty("childNodeCount")
  public val childNodeCount: Int,
)
