package ai.platon.cdt.kt.protocol.types.log

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.types.runtime.RemoteObject
import ai.platon.cdt.kt.protocol.types.runtime.StackTrace
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.Int
import kotlin.String
import kotlin.collections.List

/**
 * Log entry.
 */
public data class LogEntry(
  @JsonProperty("source")
  public val source: LogEntrySource,
  @JsonProperty("level")
  public val level: LogEntryLevel,
  @JsonProperty("text")
  public val text: String,
  @JsonProperty("timestamp")
  public val timestamp: Double,
  @JsonProperty("url")
  @Optional
  public val url: String? = null,
  @JsonProperty("lineNumber")
  @Optional
  public val lineNumber: Int? = null,
  @JsonProperty("stackTrace")
  @Optional
  public val stackTrace: StackTrace? = null,
  @JsonProperty("networkRequestId")
  @Optional
  public val networkRequestId: String? = null,
  @JsonProperty("workerId")
  @Optional
  public val workerId: String? = null,
  @JsonProperty("args")
  @Optional
  public val args: List<RemoteObject>? = null,
)
