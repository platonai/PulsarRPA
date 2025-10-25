@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.backgroundservice

import ai.platon.cdt.kt.protocol.types.backgroundservice.BackgroundServiceEvent
import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Called with all existing backgroundServiceEvents when enabled, and all new
 * events afterwards if enabled and recording.
 */
data class BackgroundServiceEventReceived(
  @param:JsonProperty("backgroundServiceEvent")
  val backgroundServiceEvent: BackgroundServiceEvent,
)
