package ai.platon.cdt.kt.protocol.types.audits

import com.fasterxml.jackson.`annotation`.JsonProperty

public data class HeavyAdIssueDetails(
  @JsonProperty("resolution")
  public val resolution: HeavyAdResolutionStatus,
  @JsonProperty("reason")
  public val reason: HeavyAdReason,
  @JsonProperty("frame")
  public val frame: AffectedFrame,
)
