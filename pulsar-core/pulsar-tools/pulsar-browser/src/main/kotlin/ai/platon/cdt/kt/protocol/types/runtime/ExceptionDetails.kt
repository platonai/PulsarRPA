package ai.platon.cdt.kt.protocol.types.runtime

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int
import kotlin.String

/**
 * Detailed information about exception (or error) that was thrown during script compilation or
 * execution.
 */
public data class ExceptionDetails(
  @JsonProperty("exceptionId")
  public val exceptionId: Int,
  @JsonProperty("text")
  public val text: String,
  @JsonProperty("lineNumber")
  public val lineNumber: Int,
  @JsonProperty("columnNumber")
  public val columnNumber: Int,
  @JsonProperty("scriptId")
  @Optional
  public val scriptId: String? = null,
  @JsonProperty("url")
  @Optional
  public val url: String? = null,
  @JsonProperty("stackTrace")
  @Optional
  public val stackTrace: StackTrace? = null,
  @JsonProperty("exception")
  @Optional
  public val exception: RemoteObject? = null,
  @JsonProperty("executionContextId")
  @Optional
  public val executionContextId: Int? = null,
)
