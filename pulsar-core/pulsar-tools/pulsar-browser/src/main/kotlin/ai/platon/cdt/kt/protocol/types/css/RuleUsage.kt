package ai.platon.cdt.kt.protocol.types.css

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.Double
import kotlin.String

/**
 * CSS coverage information.
 */
public data class RuleUsage(
  @JsonProperty("styleSheetId")
  public val styleSheetId: String,
  @JsonProperty("startOffset")
  public val startOffset: Double,
  @JsonProperty("endOffset")
  public val endOffset: Double,
  @JsonProperty("used")
  public val used: Boolean,
)
