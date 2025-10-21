package ai.platon.cdt.kt.protocol.types.debugger

import com.fasterxml.jackson.`annotation`.JsonProperty

public enum class BreakLocationType {
  @JsonProperty("debuggerStatement")
  DEBUGGER_STATEMENT,
  @JsonProperty("call")
  CALL,
  @JsonProperty("return")
  RETURN,
}
