@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.webaudio

import ai.platon.cdt.kt.protocol.types.webaudio.BaseAudioContext
import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Notifies that a new BaseAudioContext has been created.
 */
data class ContextCreated(
  @param:JsonProperty("context")
  val context: BaseAudioContext,
)
