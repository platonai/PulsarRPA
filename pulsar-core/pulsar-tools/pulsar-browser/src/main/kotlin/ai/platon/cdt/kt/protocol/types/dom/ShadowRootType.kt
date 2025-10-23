@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.dom

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Shadow root type.
 */
public enum class ShadowRootType {
  @JsonProperty("user-agent")
  USER_AGENT,
  @JsonProperty("open")
  OPEN,
  @JsonProperty("closed")
  CLOSED,
}
