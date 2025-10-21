package ai.platon.cdt.kt.protocol.events.backgroundservice

import ai.platon.cdt.kt.protocol.types.backgroundservice.BackgroundServiceEvent
import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Called with all existing backgroundServiceEvents when enabled, and all new
 * events afterwards if enabled and recording.
 */
public data class BackgroundServiceEventReceived(
  @JsonProperty("backgroundServiceEvent")
  public val backgroundServiceEvent: BackgroundServiceEvent,
)
