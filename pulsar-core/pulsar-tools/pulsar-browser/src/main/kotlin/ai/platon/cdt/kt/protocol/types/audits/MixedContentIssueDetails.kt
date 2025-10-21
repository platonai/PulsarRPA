package ai.platon.cdt.kt.protocol.types.audits

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

public data class MixedContentIssueDetails(
  @JsonProperty("resourceType")
  @Optional
  public val resourceType: MixedContentResourceType? = null,
  @JsonProperty("resolutionStatus")
  public val resolutionStatus: MixedContentResolutionStatus,
  @JsonProperty("insecureURL")
  public val insecureURL: String,
  @JsonProperty("mainResourceURL")
  public val mainResourceURL: String,
  @JsonProperty("request")
  @Optional
  public val request: AffectedRequest? = null,
  @JsonProperty("frame")
  @Optional
  public val frame: AffectedFrame? = null,
)
