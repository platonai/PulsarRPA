@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.commands

import ai.platon.cdt.kt.protocol.events.cast.IssueUpdated
import ai.platon.cdt.kt.protocol.events.cast.SinksUpdated
import ai.platon.cdt.kt.protocol.support.annotations.EventName
import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.support.annotations.ParamName
import ai.platon.cdt.kt.protocol.support.types.EventHandler
import ai.platon.cdt.kt.protocol.support.types.EventListener
import kotlin.String
import kotlin.Unit

/**
 * A domain for interacting with Cast, Presentation API, and Remote Playback API
 * functionalities.
 */
@Experimental
interface Cast {
  /**
   * Starts observing for sinks that can be used for tab mirroring, and if set,
   * sinks compatible with |presentationUrl| as well. When sinks are found, a
   * |sinksUpdated| event is fired.
   * Also starts observing for issue messages. When an issue is added or removed,
   * an |issueUpdated| event is fired.
   * @param presentationUrl
   */
  suspend fun enable(@ParamName("presentationUrl") @Optional presentationUrl: String? = null)

  suspend fun enable() {
    return enable(null)
  }

  /**
   * Stops observing for sinks and issues.
   */
  suspend fun disable()

  /**
   * Sets a sink to be used when the web page requests the browser to choose a
   * sink via Presentation API, Remote Playback API, or Cast SDK.
   * @param sinkName
   */
  suspend fun setSinkToUse(@ParamName("sinkName") sinkName: String)

  /**
   * Starts mirroring the tab to the sink.
   * @param sinkName
   */
  suspend fun startTabMirroring(@ParamName("sinkName") sinkName: String)

  /**
   * Stops the active Cast session on the sink.
   * @param sinkName
   */
  suspend fun stopCasting(@ParamName("sinkName") sinkName: String)

  @EventName("sinksUpdated")
  fun onSinksUpdated(eventListener: EventHandler<SinksUpdated>): EventListener

  @EventName("sinksUpdated")
  fun onSinksUpdated(eventListener: suspend (SinksUpdated) -> Unit): EventListener

  @EventName("issueUpdated")
  fun onIssueUpdated(eventListener: EventHandler<IssueUpdated>): EventListener

  @EventName("issueUpdated")
  fun onIssueUpdated(eventListener: suspend (IssueUpdated) -> Unit): EventListener
}
