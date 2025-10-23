@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.webaudio

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * Protocol object for AudioListener
 */
data class AudioListener(
  @param:JsonProperty("listenerId")
  val listenerId: String,
  @param:JsonProperty("contextId")
  val contextId: String,
)
