@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.webaudio

import ai.platon.cdt.kt.protocol.types.webaudio.AudioParam
import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Notifies that a new AudioParam has been created.
 */
data class AudioParamCreated(
  @param:JsonProperty("param")
  val `param`: AudioParam,
)
