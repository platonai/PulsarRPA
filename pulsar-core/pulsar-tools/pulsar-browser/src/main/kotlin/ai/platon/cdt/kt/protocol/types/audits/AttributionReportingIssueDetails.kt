package ai.platon.cdt.kt.protocol.types.audits

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int
import kotlin.String

/**
 * Details for issues around "Attribution Reporting API" usage.
 * Explainer: https://github.com/WICG/conversion-measurement-api
 */
public data class AttributionReportingIssueDetails(
  @JsonProperty("violationType")
  public val violationType: AttributionReportingIssueType,
  @JsonProperty("frame")
  @Optional
  public val frame: AffectedFrame? = null,
  @JsonProperty("request")
  @Optional
  public val request: AffectedRequest? = null,
  @JsonProperty("violatingNodeId")
  @Optional
  public val violatingNodeId: Int? = null,
  @JsonProperty("invalidParameter")
  @Optional
  public val invalidParameter: String? = null,
)
