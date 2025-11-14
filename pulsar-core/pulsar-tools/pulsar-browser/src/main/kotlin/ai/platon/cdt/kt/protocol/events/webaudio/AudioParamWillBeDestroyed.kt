@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.webaudio

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * Notifies that an existing AudioParam has been destroyed.
 */
data class AudioParamWillBeDestroyed(
  @param:JsonProperty("contextId")
  val contextId: String,
  @param:JsonProperty("nodeId")
  val nodeId: String,
  @param:JsonProperty("paramId")
  val paramId: String,
)
