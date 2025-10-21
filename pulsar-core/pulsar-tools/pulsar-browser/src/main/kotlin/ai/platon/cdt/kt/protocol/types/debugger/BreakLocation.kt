package ai.platon.cdt.kt.protocol.types.debugger

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int
import kotlin.String

public data class BreakLocation(
  @JsonProperty("scriptId")
  public val scriptId: String,
  @JsonProperty("lineNumber")
  public val lineNumber: Int,
  @JsonProperty("columnNumber")
  @Optional
  public val columnNumber: Int? = null,
  @JsonProperty("type")
  @Optional
  public val type: BreakLocationType? = null,
)
