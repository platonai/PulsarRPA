package ai.platon.cdt.kt.protocol.events.overlay

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int

/**
 * Fired when the node should be inspected. This happens after call to `setInspectMode` or when
 * user manually inspects an element.
 */
public data class InspectNodeRequested(
  @JsonProperty("backendNodeId")
  public val backendNodeId: Int,
)
