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
public interface Memory {
  public suspend fun getDOMCounters(): DOMCounters

  public suspend fun prepareForLeakDetection()

  /**
   * Simulate OomIntervention by purging V8 memory.
   */
  public suspend fun forciblyPurgeJavaScriptMemory()

  /**
   * Enable/disable suppressing memory pressure notifications in all processes.
   * @param suppressed If true, memory pressure notifications will be suppressed.
   */
  public suspend fun setPressureNotificationsSuppressed(@ParamName("suppressed")
      suppressed: Boolean)

  /**
   * Simulate a memory pressure notification in all processes.
   * @param level Memory pressure level of the notification.
   */
  public suspend fun simulatePressureNotification(@ParamName("level") level: PressureLevel)

  /**
   * Start collecting native memory profile.
   * @param samplingInterval Average number of bytes between samples.
   * @param suppressRandomness Do not randomize intervals between samples.
   */
  public suspend fun startSampling(@ParamName("samplingInterval") @Optional samplingInterval: Int?,
      @ParamName("suppressRandomness") @Optional suppressRandomness: Boolean?)

  public suspend fun startSampling() {
    return startSampling(null, null)
  }

  /**
   * Stop collecting native memory profile.
   */
  public suspend fun stopSampling()

  /**
   * Retrieve native memory allocations profile
   * collected since renderer process startup.
   */
  @Returns("profile")
  public suspend fun getAllTimeSamplingProfile(): SamplingProfile

  /**
   * Retrieve native memory allocations profile
   * collected since browser process startup.
   */
  @Returns("profile")
  public suspend fun getBrowserSamplingProfile(): SamplingProfile

  /**
   * Retrieve native memory allocations profile collected since last
   * `startSampling` call.
   */
  @Returns("profile")
  public suspend fun getSamplingProfile(): SamplingProfile
}
