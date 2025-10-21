package ai.platon.cdt.kt.protocol.types.audits

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int
import kotlin.String

public data class SourceCodeLocation(
  @JsonProperty("scriptId")
  @Optional
  public val scriptId: String? = null,
  @JsonProperty("url")
  public val url: String,
  @JsonProperty("lineNumber")
  public val lineNumber: Int,
  @JsonProperty("columnNumber")
  public val columnNumber: Int,
)
