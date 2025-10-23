@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.page

import com.fasterxml.jackson.`annotation`.JsonProperty

public enum class GatedAPIFeatures {
  @JsonProperty("SharedArrayBuffers")
  SHARED_ARRAY_BUFFERS,
  @JsonProperty("SharedArrayBuffersTransferAllowed")
  SHARED_ARRAY_BUFFERS_TRANSFER_ALLOWED,
  @JsonProperty("PerformanceMeasureMemory")
  PERFORMANCE_MEASURE_MEMORY,
  @JsonProperty("PerformanceProfile")
  PERFORMANCE_PROFILE,
}
