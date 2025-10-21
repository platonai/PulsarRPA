package ai.platon.cdt.kt.protocol.types.runtime

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int
import kotlin.String

/**
 * Stack entry for runtime errors and assertions.
 */
public data class CallFrame(
  @JsonProperty("functionName")
  public val functionName: String,
  @JsonProperty("scriptId")
  public val scriptId: String,
  @JsonProperty("url")
  public val url: String,
  @JsonProperty("lineNumber")
  public val lineNumber: Int,
  @JsonProperty("columnNumber")
  public val columnNumber: Int,
)
