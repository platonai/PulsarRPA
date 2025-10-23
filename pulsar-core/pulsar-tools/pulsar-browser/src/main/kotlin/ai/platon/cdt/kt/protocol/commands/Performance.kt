@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.commands

import ai.platon.cdt.kt.protocol.events.performance.Metrics
import ai.platon.cdt.kt.protocol.support.annotations.EventName
import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.support.annotations.ParamName
import ai.platon.cdt.kt.protocol.support.annotations.ReturnTypeParameter
import ai.platon.cdt.kt.protocol.support.annotations.Returns
import ai.platon.cdt.kt.protocol.support.types.EventHandler
import ai.platon.cdt.kt.protocol.support.types.EventListener
import ai.platon.cdt.kt.protocol.types.performance.EnableTimeDomain
import ai.platon.cdt.kt.protocol.types.performance.Metric
import ai.platon.cdt.kt.protocol.types.performance.SetTimeDomainTimeDomain
import kotlin.Deprecated
import kotlin.Unit
import kotlin.collections.List

interface Performance {
  /**
   * Disable collecting and reporting metrics.
   */
  suspend fun disable()

  /**
   * Enable collecting and reporting metrics.
   * @param timeDomain Time domain to use for collecting and reporting duration metrics.
   */
  suspend fun enable(@ParamName("timeDomain") @Optional timeDomain: EnableTimeDomain? = null)

  suspend fun enable() {
    return enable(null)
  }

  /**
   * Sets time domain to use for collecting and reporting duration metrics.
   * Note that this must be called before enabling metrics collection. Calling
   * this method while metrics collection is enabled returns an error.
   * @param timeDomain Time domain
   */
  @Deprecated("Deprecated by protocol")
  @Experimental
  suspend fun setTimeDomain(@ParamName("timeDomain") timeDomain: SetTimeDomainTimeDomain)

  /**
   * Retrieve current values of run-time metrics.
   */
  @Returns("metrics")
  @ReturnTypeParameter(Metric::class)
  suspend fun getMetrics(): List<Metric>

  @EventName("metrics")
  fun onMetrics(eventListener: EventHandler<Metrics>): EventListener

  @EventName("metrics")
  fun onMetrics(eventListener: suspend (Metrics) -> Unit): EventListener
}
