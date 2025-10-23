@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.performance

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Time domain to use for collecting and reporting duration metrics.
 */
public enum class EnableTimeDomain {
  @JsonProperty("timeTicks")
  TIME_TICKS,
  @JsonProperty("threadTicks")
  THREAD_TICKS,
}
