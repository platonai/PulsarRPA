package ai.platon.cdt.kt.protocol.types.webaudio

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Enum of BaseAudioContext types
 */
public enum class ContextType {
  @JsonProperty("realtime")
  REALTIME,
  @JsonProperty("offline")
  OFFLINE,
}
