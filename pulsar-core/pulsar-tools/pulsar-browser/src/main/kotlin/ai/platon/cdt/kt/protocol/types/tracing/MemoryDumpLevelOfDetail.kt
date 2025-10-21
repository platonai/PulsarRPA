package ai.platon.cdt.kt.protocol.types.tracing

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Details exposed when memory request explicitly declared.
 * Keep consistent with memory_dump_request_args.h and
 * memory_instrumentation.mojom
 */
public enum class MemoryDumpLevelOfDetail {
  @JsonProperty("background")
  BACKGROUND,
  @JsonProperty("light")
  LIGHT,
  @JsonProperty("detailed")
  DETAILED,
}
