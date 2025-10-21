package ai.platon.cdt.kt.protocol.types.debugger

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.String

/**
 * Search match for resource.
 */
public data class SearchMatch(
  @JsonProperty("lineNumber")
  public val lineNumber: Double,
  @JsonProperty("lineContent")
  public val lineContent: String,
)
