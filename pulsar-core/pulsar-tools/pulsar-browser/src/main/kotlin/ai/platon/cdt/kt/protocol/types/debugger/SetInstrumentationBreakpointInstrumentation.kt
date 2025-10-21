package ai.platon.cdt.kt.protocol.types.debugger

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Instrumentation name.
 */
public enum class SetInstrumentationBreakpointInstrumentation {
  @JsonProperty("beforeScriptExecution")
  BEFORE_SCRIPT_EXECUTION,
  @JsonProperty("beforeScriptWithSourceMapExecution")
  BEFORE_SCRIPT_WITH_SOURCE_MAP_EXECUTION,
}
