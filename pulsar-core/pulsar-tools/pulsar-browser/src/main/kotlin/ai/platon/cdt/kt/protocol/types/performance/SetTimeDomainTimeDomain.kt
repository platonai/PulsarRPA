package ai.platon.cdt.kt.protocol.types.performance

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Time domain
 */
public enum class SetTimeDomainTimeDomain {
  @JsonProperty("timeTicks")
  TIME_TICKS,
  @JsonProperty("threadTicks")
  THREAD_TICKS,
}
