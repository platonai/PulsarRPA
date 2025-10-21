package ai.platon.cdt.kt.protocol.types.audits

import com.fasterxml.jackson.`annotation`.JsonProperty

public enum class HeavyAdReason {
  @JsonProperty("NetworkTotalLimit")
  NETWORK_TOTAL_LIMIT,
  @JsonProperty("CpuTotalLimit")
  CPU_TOTAL_LIMIT,
  @JsonProperty("CpuPeakLimit")
  CPU_PEAK_LIMIT,
}
