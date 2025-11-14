@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.commands

import ai.platon.cdt.kt.protocol.events.tethering.Accepted
import ai.platon.cdt.kt.protocol.support.annotations.EventName
import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.ParamName
import ai.platon.cdt.kt.protocol.support.types.EventHandler
import ai.platon.cdt.kt.protocol.support.types.EventListener
import kotlin.Int
import kotlin.Unit

/**
 * The Tethering domain defines methods and events for browser port binding.
 */
@Experimental
interface Tethering {
  /**
   * Request browser port binding.
   * @param port Port number to bind.
   */
  suspend fun bind(@ParamName("port") port: Int)

  /**
   * Request browser port unbinding.
   * @param port Port number to unbind.
   */
  suspend fun unbind(@ParamName("port") port: Int)

  @EventName("accepted")
  fun onAccepted(eventListener: EventHandler<Accepted>): EventListener

  @EventName("accepted")
  fun onAccepted(eventListener: suspend (Accepted) -> Unit): EventListener
}
