@file:Suppress("unused")
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
data class LogEntry(
  @param:JsonProperty("source")
  val source: LogEntrySource,
  @param:JsonProperty("level")
  val level: LogEntryLevel,
  @param:JsonProperty("text")
  val text: String,
  @param:JsonProperty("timestamp")
  val timestamp: Double,
  @param:JsonProperty("url")
  @param:Optional
  val url: String? = null,
  @param:JsonProperty("lineNumber")
  @param:Optional
  val lineNumber: Int? = null,
  @param:JsonProperty("stackTrace")
  @param:Optional
  val stackTrace: StackTrace? = null,
  @param:JsonProperty("networkRequestId")
  @param:Optional
  val networkRequestId: String? = null,
  @param:JsonProperty("workerId")
  @param:Optional
  val workerId: String? = null,
  @param:JsonProperty("args")
  @param:Optional
  val args: List<RemoteObject>? = null,
)
