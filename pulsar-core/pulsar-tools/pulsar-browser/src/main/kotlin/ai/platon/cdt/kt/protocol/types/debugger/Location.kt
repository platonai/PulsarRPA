package ai.platon.cdt.kt.protocol.types.debugger

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int
import kotlin.String

/**
 * Location in the source code.
 */
public data class Location(
  @JsonProperty("scriptId")
  public val scriptId: String,
  @JsonProperty("lineNumber")
  public val lineNumber: Int,
  @JsonProperty("columnNumber")
  @Optional
  public val columnNumber: Int? = null,
)
