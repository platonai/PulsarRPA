@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.runtime

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int
import kotlin.String

/**
 * Detailed information about exception (or error) that was thrown during script compilation or
 * execution.
 */
data class ExceptionDetails(
  @param:JsonProperty("exceptionId")
  val exceptionId: Int,
  @param:JsonProperty("text")
  val text: String,
  @param:JsonProperty("lineNumber")
  val lineNumber: Int,
  @param:JsonProperty("columnNumber")
  val columnNumber: Int,
  @param:JsonProperty("scriptId")
  @param:Optional
  val scriptId: String? = null,
  @param:JsonProperty("url")
  @param:Optional
  val url: String? = null,
  @param:JsonProperty("stackTrace")
  @param:Optional
  val stackTrace: StackTrace? = null,
  @param:JsonProperty("exception")
  @param:Optional
  val exception: RemoteObject? = null,
  @param:JsonProperty("executionContextId")
  @param:Optional
  val executionContextId: Int? = null,
)
