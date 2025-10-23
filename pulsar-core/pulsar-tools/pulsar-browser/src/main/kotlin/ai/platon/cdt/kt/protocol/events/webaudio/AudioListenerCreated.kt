@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.webaudio

import ai.platon.cdt.kt.protocol.types.webaudio.AudioListener
import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Notifies that the construction of an AudioListener has finished.
 */
data class AudioListenerCreated(
  @param:JsonProperty("listener")
  val listener: AudioListener,
)
