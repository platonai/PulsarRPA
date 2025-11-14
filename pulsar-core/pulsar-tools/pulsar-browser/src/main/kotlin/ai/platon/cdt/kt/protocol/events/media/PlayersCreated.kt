@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.media

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String
import kotlin.collections.List

/**
 * Called whenever a player is created, or when a new agent joins and receives
 * a list of active players. If an agent is restored, it will receive the full
 * list of player ids and all events again.
 */
data class PlayersCreated(
  @param:JsonProperty("players")
  val players: List<String>,
)
