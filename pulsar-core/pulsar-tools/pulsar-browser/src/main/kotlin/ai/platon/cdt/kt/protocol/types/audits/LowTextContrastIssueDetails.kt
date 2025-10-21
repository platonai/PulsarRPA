package ai.platon.cdt.kt.protocol.types.audits

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.Int
import kotlin.String

public data class LowTextContrastIssueDetails(
  @JsonProperty("violatingNodeId")
  public val violatingNodeId: Int,
  @JsonProperty("violatingNodeSelector")
  public val violatingNodeSelector: String,
  @JsonProperty("contrastRatio")
  public val contrastRatio: Double,
  @JsonProperty("thresholdAA")
  public val thresholdAA: Double,
  @JsonProperty("thresholdAAA")
  public val thresholdAAA: Double,
  @JsonProperty("fontSize")
  public val fontSize: String,
  @JsonProperty("fontWeight")
  public val fontWeight: String,
)
