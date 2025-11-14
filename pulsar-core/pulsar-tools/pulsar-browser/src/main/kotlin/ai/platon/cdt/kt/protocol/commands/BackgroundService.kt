@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.commands

import ai.platon.cdt.kt.protocol.events.backgroundservice.BackgroundServiceEventReceived
import ai.platon.cdt.kt.protocol.events.backgroundservice.RecordingStateChanged
import ai.platon.cdt.kt.protocol.support.annotations.EventName
import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.ParamName
import ai.platon.cdt.kt.protocol.support.types.EventHandler
import ai.platon.cdt.kt.protocol.support.types.EventListener
import ai.platon.cdt.kt.protocol.types.backgroundservice.ServiceName
import kotlin.Boolean
import kotlin.Unit

/**
 * Defines events for background web platform features.
 */
@Experimental
interface BackgroundService {
  /**
   * Enables event updates for the service.
   * @param service
   */
  suspend fun startObserving(@ParamName("service") service: ServiceName)

  /**
   * Disables event updates for the service.
   * @param service
   */
  suspend fun stopObserving(@ParamName("service") service: ServiceName)

  /**
   * Set the recording state for the service.
   * @param shouldRecord
   * @param service
   */
  suspend fun setRecording(@ParamName("shouldRecord") shouldRecord: Boolean, @ParamName("service") service: ServiceName)

  /**
   * Clears all stored data for the service.
   * @param service
   */
  suspend fun clearEvents(@ParamName("service") service: ServiceName)

  @EventName("recordingStateChanged")
  fun onRecordingStateChanged(eventListener: EventHandler<RecordingStateChanged>): EventListener

  @EventName("recordingStateChanged")
  fun onRecordingStateChanged(eventListener: suspend (RecordingStateChanged) -> Unit): EventListener

  @EventName("backgroundServiceEventReceived")
  fun onBackgroundServiceEventReceived(eventListener: EventHandler<BackgroundServiceEventReceived>): EventListener

  @EventName("backgroundServiceEventReceived")
  fun onBackgroundServiceEventReceived(eventListener: suspend (BackgroundServiceEventReceived) -> Unit): EventListener
}
