@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.commands

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.support.annotations.ParamName
import ai.platon.cdt.kt.protocol.support.annotations.Returns
import ai.platon.cdt.kt.protocol.types.memory.DOMCounters
import ai.platon.cdt.kt.protocol.types.memory.PressureLevel
import ai.platon.cdt.kt.protocol.types.memory.SamplingProfile
import kotlin.Boolean
import kotlin.Int

@Experimental
interface Memory {
  suspend fun getDOMCounters(): DOMCounters

  suspend fun prepareForLeakDetection()

  /**
   * Simulate OomIntervention by purging V8 memory.
   */
  suspend fun forciblyPurgeJavaScriptMemory()

  /**
   * Enable/disable suppressing memory pressure notifications in all processes.
   * @param suppressed If true, memory pressure notifications will be suppressed.
   */
  suspend fun setPressureNotificationsSuppressed(@ParamName("suppressed") suppressed: Boolean)

  /**
   * Simulate a memory pressure notification in all processes.
   * @param level Memory pressure level of the notification.
   */
  suspend fun simulatePressureNotification(@ParamName("level") level: PressureLevel)

  /**
   * Start collecting native memory profile.
   * @param samplingInterval Average number of bytes between samples.
   * @param suppressRandomness Do not randomize intervals between samples.
   */
  suspend fun startSampling(@ParamName("samplingInterval") @Optional samplingInterval: Int? = null, @ParamName("suppressRandomness") @Optional suppressRandomness: Boolean? = null)

  suspend fun startSampling() {
    return startSampling(null, null)
  }

  /**
   * Stop collecting native memory profile.
   */
  suspend fun stopSampling()

  /**
   * Retrieve native memory allocations profile
   * collected since renderer process startup.
   */
  @Returns("profile")
  suspend fun getAllTimeSamplingProfile(): SamplingProfile

  /**
   * Retrieve native memory allocations profile
   * collected since browser process startup.
   */
  @Returns("profile")
  suspend fun getBrowserSamplingProfile(): SamplingProfile

  /**
   * Retrieve native memory allocations profile collected since last
   * `startSampling` call.
   */
  @Returns("profile")
  suspend fun getSamplingProfile(): SamplingProfile
}
