@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.overlay

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int

/**
 * Fired when the node should be inspected. This happens after call to `setInspectMode` or when
 * user manually inspects an element.
 */
data class InspectNodeRequested(
  @param:JsonProperty("backendNodeId")
  val backendNodeId: Int,
)
