package ai.platon.cdt.kt.protocol.commands

import ai.platon.cdt.kt.protocol.events.log.EntryAdded
import ai.platon.cdt.kt.protocol.support.annotations.EventName
import ai.platon.cdt.kt.protocol.support.annotations.ParamName
import ai.platon.cdt.kt.protocol.support.types.EventHandler
import ai.platon.cdt.kt.protocol.support.types.EventListener
import ai.platon.cdt.kt.protocol.types.log.ViolationSetting
import kotlin.Unit
import kotlin.collections.List

/**
 * Provides access to log entries.
 */
public interface Log {
  /**
   * Clears the log.
   */
  public suspend fun clear()

  /**
   * Disables log domain, prevents further log entries from being reported to the client.
   */
  public suspend fun disable()

  /**
   * Enables log domain, sends the entries collected so far to the client by means of the
   * `entryAdded` notification.
   */
  public suspend fun enable()

  /**
   * start violation reporting.
   * @param config Configuration for violations.
   */
  public suspend fun startViolationsReport(@ParamName("config") config: List<ViolationSetting>)

  /**
   * Stop violation reporting.
   */
  public suspend fun stopViolationsReport()

  @EventName("entryAdded")
  public fun onEntryAdded(eventListener: EventHandler<EntryAdded>): EventListener

  @EventName("entryAdded")
  public fun onEntryAdded(eventListener: suspend (EntryAdded) -> Unit): EventListener
}
