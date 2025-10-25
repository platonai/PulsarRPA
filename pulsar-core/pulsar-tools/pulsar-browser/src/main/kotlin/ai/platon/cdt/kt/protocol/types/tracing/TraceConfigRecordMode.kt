@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.tracing

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Controls how the trace buffer stores data.
 */
public enum class TraceConfigRecordMode {
  @JsonProperty("recordUntilFull")
  RECORD_UNTIL_FULL,
  @JsonProperty("recordContinuously")
  RECORD_CONTINUOUSLY,
  @JsonProperty("recordAsMuchAsPossible")
  RECORD_AS_MUCH_AS_POSSIBLE,
  @JsonProperty("echoToConsole")
  ECHO_TO_CONSOLE,
}
