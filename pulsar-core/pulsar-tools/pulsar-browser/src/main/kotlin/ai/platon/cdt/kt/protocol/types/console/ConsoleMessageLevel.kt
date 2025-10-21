package ai.platon.cdt.kt.protocol.types.console

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Message severity.
 */
public enum class ConsoleMessageLevel {
  @JsonProperty("log")
  LOG,
  @JsonProperty("warning")
  WARNING,
  @JsonProperty("error")
  ERROR,
  @JsonProperty("debug")
  DEBUG,
  @JsonProperty("info")
  INFO,
}
