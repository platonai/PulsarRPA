package ai.platon.cdt.kt.protocol.commands

import ai.platon.cdt.kt.protocol.events.serviceworker.WorkerErrorReported
import ai.platon.cdt.kt.protocol.events.serviceworker.WorkerRegistrationUpdated
import ai.platon.cdt.kt.protocol.events.serviceworker.WorkerVersionUpdated
import ai.platon.cdt.kt.protocol.support.annotations.EventName
import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.ParamName
import ai.platon.cdt.kt.protocol.support.types.EventHandler
import ai.platon.cdt.kt.protocol.support.types.EventListener
import kotlin.Boolean
import kotlin.String
import kotlin.Unit

@Experimental
public interface ServiceWorker {
  /**
   * @param origin
   * @param registrationId
   * @param data
   */
  public suspend fun deliverPushMessage(
    @ParamName("origin") origin: String,
    @ParamName("registrationId") registrationId: String,
    @ParamName("data") `data`: String,
  )

  public suspend fun disable()

  /**
   * @param origin
   * @param registrationId
   * @param tag
   * @param lastChance
   */
  public suspend fun dispatchSyncEvent(
    @ParamName("origin") origin: String,
    @ParamName("registrationId") registrationId: String,
    @ParamName("tag") tag: String,
    @ParamName("lastChance") lastChance: Boolean,
  )

  /**
   * @param origin
   * @param registrationId
   * @param tag
   */
  public suspend fun dispatchPeriodicSyncEvent(
    @ParamName("origin") origin: String,
    @ParamName("registrationId") registrationId: String,
    @ParamName("tag") tag: String,
  )

  public suspend fun enable()

  /**
   * @param versionId
   */
  public suspend fun inspectWorker(@ParamName("versionId") versionId: String)

  /**
   * @param forceUpdateOnPageLoad
   */
  public suspend fun setForceUpdateOnPageLoad(@ParamName("forceUpdateOnPageLoad")
      forceUpdateOnPageLoad: Boolean)

  /**
   * @param scopeURL
   */
  public suspend fun skipWaiting(@ParamName("scopeURL") scopeURL: String)

  /**
   * @param scopeURL
   */
  public suspend fun startWorker(@ParamName("scopeURL") scopeURL: String)

  public suspend fun stopAllWorkers()

  /**
   * @param versionId
   */
  public suspend fun stopWorker(@ParamName("versionId") versionId: String)

  /**
   * @param scopeURL
   */
  public suspend fun unregister(@ParamName("scopeURL") scopeURL: String)

  /**
   * @param scopeURL
   */
  public suspend fun updateRegistration(@ParamName("scopeURL") scopeURL: String)

  @EventName("workerErrorReported")
  public fun onWorkerErrorReported(eventListener: EventHandler<WorkerErrorReported>): EventListener

  @EventName("workerErrorReported")
  public fun onWorkerErrorReported(eventListener: suspend (WorkerErrorReported) -> Unit):
      EventListener

  @EventName("workerRegistrationUpdated")
  public fun onWorkerRegistrationUpdated(eventListener: EventHandler<WorkerRegistrationUpdated>):
      EventListener

  @EventName("workerRegistrationUpdated")
  public
      fun onWorkerRegistrationUpdated(eventListener: suspend (WorkerRegistrationUpdated) -> Unit):
      EventListener

  @EventName("workerVersionUpdated")
  public fun onWorkerVersionUpdated(eventListener: EventHandler<WorkerVersionUpdated>):
      EventListener

  @EventName("workerVersionUpdated")
  public fun onWorkerVersionUpdated(eventListener: suspend (WorkerVersionUpdated) -> Unit):
      EventListener
}
