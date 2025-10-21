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
public interface BackgroundService {
  /**
   * Enables event updates for the service.
   * @param service
   */
  public suspend fun startObserving(@ParamName("service") service: ServiceName)

  /**
   * Disables event updates for the service.
   * @param service
   */
  public suspend fun stopObserving(@ParamName("service") service: ServiceName)

  /**
   * Set the recording state for the service.
   * @param shouldRecord
   * @param service
   */
  public suspend fun setRecording(@ParamName("shouldRecord") shouldRecord: Boolean,
      @ParamName("service") service: ServiceName)

  /**
   * Clears all stored data for the service.
   * @param service
   */
  public suspend fun clearEvents(@ParamName("service") service: ServiceName)

  @EventName("recordingStateChanged")
  public fun onRecordingStateChanged(eventListener: EventHandler<RecordingStateChanged>):
      EventListener

  @EventName("recordingStateChanged")
  public fun onRecordingStateChanged(eventListener: suspend (RecordingStateChanged) -> Unit):
      EventListener

  @EventName("backgroundServiceEventReceived")
  public
      fun onBackgroundServiceEventReceived(eventListener: EventHandler<BackgroundServiceEventReceived>):
      EventListener

  @EventName("backgroundServiceEventReceived")
  public
      fun onBackgroundServiceEventReceived(eventListener: suspend (BackgroundServiceEventReceived) -> Unit):
      EventListener
}
