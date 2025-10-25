@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.webaudio

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * Notifies that an existing BaseAudioContext will be destroyed.
 */
data class ContextWillBeDestroyed(
  @param:JsonProperty("contextId")
  val contextId: String,
)
