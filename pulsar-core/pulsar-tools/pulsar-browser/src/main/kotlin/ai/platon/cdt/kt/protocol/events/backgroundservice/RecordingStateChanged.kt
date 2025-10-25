@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.backgroundservice

import ai.platon.cdt.kt.protocol.types.backgroundservice.ServiceName
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean

/**
 * Called when the recording state for the service has been updated.
 */
data class RecordingStateChanged(
  @param:JsonProperty("isRecording")
  val isRecording: Boolean,
  @param:JsonProperty("service")
  val service: ServiceName,
)
