@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.commands

import ai.platon.cdt.kt.protocol.events.inspector.Detached
import ai.platon.cdt.kt.protocol.events.inspector.TargetCrashed
import ai.platon.cdt.kt.protocol.events.inspector.TargetReloadedAfterCrash
import ai.platon.cdt.kt.protocol.support.annotations.EventName
import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.types.EventHandler
import ai.platon.cdt.kt.protocol.support.types.EventListener
import kotlin.Unit

@Experimental
interface Inspector {
  /**
   * Disables inspector domain notifications.
   */
  suspend fun disable()

  /**
   * Enables inspector domain notifications.
   */
  suspend fun enable()

  @EventName("detached")
  fun onDetached(eventListener: EventHandler<Detached>): EventListener

  @EventName("detached")
  fun onDetached(eventListener: suspend (Detached) -> Unit): EventListener

  @EventName("targetCrashed")
  fun onTargetCrashed(eventListener: EventHandler<TargetCrashed>): EventListener

  @EventName("targetCrashed")
  fun onTargetCrashed(eventListener: suspend (TargetCrashed) -> Unit): EventListener

  @EventName("targetReloadedAfterCrash")
  fun onTargetReloadedAfterCrash(eventListener: EventHandler<TargetReloadedAfterCrash>): EventListener

  @EventName("targetReloadedAfterCrash")
  fun onTargetReloadedAfterCrash(eventListener: suspend (TargetReloadedAfterCrash) -> Unit): EventListener
}
