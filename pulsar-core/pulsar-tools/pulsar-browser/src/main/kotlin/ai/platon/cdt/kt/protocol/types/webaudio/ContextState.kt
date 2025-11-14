@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.webaudio

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Enum of AudioContextState from the spec
 */
public enum class ContextState {
  @JsonProperty("suspended")
  SUSPENDED,
  @JsonProperty("running")
  RUNNING,
  @JsonProperty("closed")
  CLOSED,
}
