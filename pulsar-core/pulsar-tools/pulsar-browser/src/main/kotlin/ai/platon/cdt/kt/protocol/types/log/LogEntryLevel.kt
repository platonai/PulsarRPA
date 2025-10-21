package ai.platon.cdt.kt.protocol.types.log

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Log entry severity.
 */
public enum class LogEntryLevel {
  @JsonProperty("verbose")
  VERBOSE,
  @JsonProperty("info")
  INFO,
  @JsonProperty("warning")
  WARNING,
  @JsonProperty("error")
  ERROR,
}
