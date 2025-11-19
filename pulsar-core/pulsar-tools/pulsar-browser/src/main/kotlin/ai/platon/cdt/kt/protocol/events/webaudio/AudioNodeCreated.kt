@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.webaudio

import ai.platon.cdt.kt.protocol.types.webaudio.AudioNode
import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Notifies that a new AudioNode has been created.
 */
data class AudioNodeCreated(
  @param:JsonProperty("node")
  val node: AudioNode,
)
