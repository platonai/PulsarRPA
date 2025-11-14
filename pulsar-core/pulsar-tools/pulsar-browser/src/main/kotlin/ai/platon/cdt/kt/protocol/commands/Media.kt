@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.commands

import ai.platon.cdt.kt.protocol.events.media.PlayerErrorsRaised
import ai.platon.cdt.kt.protocol.events.media.PlayerEventsAdded
import ai.platon.cdt.kt.protocol.events.media.PlayerMessagesLogged
import ai.platon.cdt.kt.protocol.events.media.PlayerPropertiesChanged
import ai.platon.cdt.kt.protocol.events.media.PlayersCreated
import ai.platon.cdt.kt.protocol.support.annotations.EventName
import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.types.EventHandler
import ai.platon.cdt.kt.protocol.support.types.EventListener
import kotlin.Unit

/**
 * This domain allows detailed inspection of media elements
 */
@Experimental
interface Media {
  /**
   * Enables the Media domain
   */
  suspend fun enable()

  /**
   * Disables the Media domain.
   */
  suspend fun disable()

  @EventName("playerPropertiesChanged")
  fun onPlayerPropertiesChanged(eventListener: EventHandler<PlayerPropertiesChanged>): EventListener

  @EventName("playerPropertiesChanged")
  fun onPlayerPropertiesChanged(eventListener: suspend (PlayerPropertiesChanged) -> Unit): EventListener

  @EventName("playerEventsAdded")
  fun onPlayerEventsAdded(eventListener: EventHandler<PlayerEventsAdded>): EventListener

  @EventName("playerEventsAdded")
  fun onPlayerEventsAdded(eventListener: suspend (PlayerEventsAdded) -> Unit): EventListener

  @EventName("playerMessagesLogged")
  fun onPlayerMessagesLogged(eventListener: EventHandler<PlayerMessagesLogged>): EventListener

  @EventName("playerMessagesLogged")
  fun onPlayerMessagesLogged(eventListener: suspend (PlayerMessagesLogged) -> Unit): EventListener

  @EventName("playerErrorsRaised")
  fun onPlayerErrorsRaised(eventListener: EventHandler<PlayerErrorsRaised>): EventListener

  @EventName("playerErrorsRaised")
  fun onPlayerErrorsRaised(eventListener: suspend (PlayerErrorsRaised) -> Unit): EventListener

  @EventName("playersCreated")
  fun onPlayersCreated(eventListener: EventHandler<PlayersCreated>): EventListener

  @EventName("playersCreated")
  fun onPlayersCreated(eventListener: suspend (PlayersCreated) -> Unit): EventListener
}
