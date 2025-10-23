@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.domdebugger

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * DOM breakpoint type.
 */
public enum class DOMBreakpointType {
  @JsonProperty("subtree-modified")
  SUBTREE_MODIFIED,
  @JsonProperty("attribute-modified")
  ATTRIBUTE_MODIFIED,
  @JsonProperty("node-removed")
  NODE_REMOVED,
}
