@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.webaudio

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Enum of AudioNode::ChannelInterpretation from the spec
 */
public enum class ChannelInterpretation {
  @JsonProperty("discrete")
  DISCRETE,
  @JsonProperty("speakers")
  SPEAKERS,
}
