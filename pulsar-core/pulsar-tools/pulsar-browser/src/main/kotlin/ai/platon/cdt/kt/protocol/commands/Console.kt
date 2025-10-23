@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.commands

import ai.platon.cdt.kt.protocol.events.console.MessageAdded
import ai.platon.cdt.kt.protocol.support.annotations.EventName
import ai.platon.cdt.kt.protocol.support.types.EventHandler
import ai.platon.cdt.kt.protocol.support.types.EventListener
import kotlin.Deprecated
import kotlin.Unit

/**
 * This domain is deprecated - use Runtime or Log instead.
 */
@Deprecated("Deprecated by protocol")
interface Console {
  /**
   * Does nothing.
   */
  suspend fun clearMessages()

  /**
   * Disables console domain, prevents further console messages from being reported to the client.
   */
  suspend fun disable()

  /**
   * Enables console domain, sends the messages collected so far to the client by means of the
   * `messageAdded` notification.
   */
  suspend fun enable()

  @EventName("messageAdded")
  fun onMessageAdded(eventListener: EventHandler<MessageAdded>): EventListener

  @EventName("messageAdded")
  fun onMessageAdded(eventListener: suspend (MessageAdded) -> Unit): EventListener
}
