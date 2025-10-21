package ai.platon.cdt.kt.protocol.commands

import ai.platon.cdt.kt.protocol.events.profiler.ConsoleProfileFinished
import ai.platon.cdt.kt.protocol.events.profiler.ConsoleProfileStarted
import ai.platon.cdt.kt.protocol.events.profiler.PreciseCoverageDeltaUpdate
import ai.platon.cdt.kt.protocol.support.annotations.EventName
import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.support.annotations.ParamName
import ai.platon.cdt.kt.protocol.support.annotations.ReturnTypeParameter
import ai.platon.cdt.kt.protocol.support.annotations.Returns
import ai.platon.cdt.kt.protocol.support.types.EventHandler
import ai.platon.cdt.kt.protocol.support.types.EventListener
import ai.platon.cdt.kt.protocol.types.profiler.CounterInfo
import ai.platon.cdt.kt.protocol.types.profiler.Profile
import ai.platon.cdt.kt.protocol.types.profiler.RuntimeCallCounterInfo
import ai.platon.cdt.kt.protocol.types.profiler.ScriptCoverage
import ai.platon.cdt.kt.protocol.types.profiler.ScriptTypeProfile
import ai.platon.cdt.kt.protocol.types.profiler.TakePreciseCoverage
import kotlin.Boolean
import kotlin.Double
import kotlin.Int
import kotlin.Unit
import kotlin.collections.List

public interface Profiler {
  public suspend fun disable()

  public suspend fun enable()

  /**
   * Collect coverage data for the current isolate. The coverage data may be incomplete due to
   * garbage collection.
   */
  @Returns("result")
  @ReturnTypeParameter(ScriptCoverage::class)
  public suspend fun getBestEffortCoverage(): List<ScriptCoverage>

  /**
   * Changes CPU profiler sampling interval. Must be called before CPU profiles recording started.
   * @param interval New sampling interval in microseconds.
   */
  public suspend fun setSamplingInterval(@ParamName("interval") interval: Int)

  public suspend fun start()

  /**
   * Enable precise code coverage. Coverage data for JavaScript executed before enabling precise
   * code
   * coverage may be incomplete. Enabling prevents running optimized code and resets execution
   * counters.
   * @param callCount Collect accurate call counts beyond simple 'covered' or 'not covered'.
   * @param detailed Collect block-based coverage.
   * @param allowTriggeredUpdates Allow the backend to send updates on its own initiative
   */
  @Returns("timestamp")
  public suspend fun startPreciseCoverage(
    @ParamName("callCount") @Optional callCount: Boolean?,
    @ParamName("detailed") @Optional detailed: Boolean?,
    @ParamName("allowTriggeredUpdates") @Optional allowTriggeredUpdates: Boolean?,
  ): Double

  @Returns("timestamp")
  public suspend fun startPreciseCoverage(): Double {
    return startPreciseCoverage(null, null, null)
  }

  /**
   * Enable type profile.
   */
  @Experimental
  public suspend fun startTypeProfile()

  @Returns("profile")
  public suspend fun stop(): Profile

  /**
   * Disable precise code coverage. Disabling releases unnecessary execution count records and
   * allows
   * executing optimized code.
   */
  public suspend fun stopPreciseCoverage()

  /**
   * Disable type profile. Disabling releases type profile data collected so far.
   */
  @Experimental
  public suspend fun stopTypeProfile()

  /**
   * Collect coverage data for the current isolate, and resets execution counters. Precise code
   * coverage needs to have started.
   */
  public suspend fun takePreciseCoverage(): TakePreciseCoverage

  /**
   * Collect type profile.
   */
  @Experimental
  @Returns("result")
  @ReturnTypeParameter(ScriptTypeProfile::class)
  public suspend fun takeTypeProfile(): List<ScriptTypeProfile>

  /**
   * Enable counters collection.
   */
  @Experimental
  public suspend fun enableCounters()

  /**
   * Disable counters collection.
   */
  @Experimental
  public suspend fun disableCounters()

  /**
   * Retrieve counters.
   */
  @Experimental
  @Returns("result")
  @ReturnTypeParameter(CounterInfo::class)
  public suspend fun getCounters(): List<CounterInfo>

  /**
   * Enable run time call stats collection.
   */
  @Experimental
  public suspend fun enableRuntimeCallStats()

  /**
   * Disable run time call stats collection.
   */
  @Experimental
  public suspend fun disableRuntimeCallStats()

  /**
   * Retrieve run time call stats.
   */
  @Experimental
  @Returns("result")
  @ReturnTypeParameter(RuntimeCallCounterInfo::class)
  public suspend fun getRuntimeCallStats(): List<RuntimeCallCounterInfo>

  @EventName("consoleProfileFinished")
  public fun onConsoleProfileFinished(eventListener: EventHandler<ConsoleProfileFinished>):
      EventListener

  @EventName("consoleProfileFinished")
  public fun onConsoleProfileFinished(eventListener: suspend (ConsoleProfileFinished) -> Unit):
      EventListener

  @EventName("consoleProfileStarted")
  public fun onConsoleProfileStarted(eventListener: EventHandler<ConsoleProfileStarted>):
      EventListener

  @EventName("consoleProfileStarted")
  public fun onConsoleProfileStarted(eventListener: suspend (ConsoleProfileStarted) -> Unit):
      EventListener

  @EventName("preciseCoverageDeltaUpdate")
  @Experimental
  public fun onPreciseCoverageDeltaUpdate(eventListener: EventHandler<PreciseCoverageDeltaUpdate>):
      EventListener

  @EventName("preciseCoverageDeltaUpdate")
  @Experimental
  public
      fun onPreciseCoverageDeltaUpdate(eventListener: suspend (PreciseCoverageDeltaUpdate) -> Unit):
      EventListener
}
