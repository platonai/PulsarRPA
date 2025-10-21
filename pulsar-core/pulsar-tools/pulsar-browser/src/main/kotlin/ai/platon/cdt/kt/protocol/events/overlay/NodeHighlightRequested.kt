package ai.platon.cdt.kt.protocol.events.overlay

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int

/**
 * Fired when the node should be highlighted. This happens after call to `setInspectMode`.
 */
public data class NodeHighlightRequested(
  @JsonProperty("nodeId")
  public val nodeId: Int,
)
