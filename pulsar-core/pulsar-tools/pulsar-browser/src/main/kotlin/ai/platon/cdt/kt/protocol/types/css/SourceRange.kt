package ai.platon.cdt.kt.protocol.types.css

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int

/**
 * Text range within a resource. All numbers are zero-based.
 */
public data class SourceRange(
  @JsonProperty("startLine")
  public val startLine: Int,
  @JsonProperty("startColumn")
  public val startColumn: Int,
  @JsonProperty("endLine")
  public val endLine: Int,
  @JsonProperty("endColumn")
  public val endColumn: Int,
)
