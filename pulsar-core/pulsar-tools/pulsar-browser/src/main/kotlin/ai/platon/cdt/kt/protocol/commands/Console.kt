package ai.platon.cdt.kt.protocol.commands

import ai.platon.cdt.kt.protocol.events.console.MessageAdded
import ai.platon.cdt.kt.protocol.support.annotations.EventName
import ai.platon.cdt.kt.protocol.support.types.EventHandler
import ai.platon.cdt.kt.protocol.support.types.EventListener
import java.lang.Deprecated
import kotlin.Unit

/**
 * This domain is deprecated - use Runtime or Log instead.
 */
@Deprecated
public interface Console {
  /**
   * Does nothing.
   */
  public suspend fun clearMessages()

  /**
   * Disables console domain, prevents further console messages from being reported to the client.
   */
  public suspend fun disable()

  /**
   * Enables console domain, sends the messages collected so far to the client by means of the
   * `messageAdded` notification.
   */
  public suspend fun enable()

  @EventName("messageAdded")
  public fun onMessageAdded(eventListener: EventHandler<MessageAdded>): EventListener

  @EventName("messageAdded")
  public fun onMessageAdded(eventListener: suspend (MessageAdded) -> Unit): EventListener
}
