@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.webaudio

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * Notifies that a new AudioListener has been created.
 */
data class AudioListenerWillBeDestroyed(
  @param:JsonProperty("contextId")
  val contextId: String,
  @param:JsonProperty("listenerId")
  val listenerId: String,
)
