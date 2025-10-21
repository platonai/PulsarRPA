package ai.platon.cdt.kt.protocol.types.audits

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * This struct holds a list of optional fields with additional information
 * specific to the kind of issue. When adding a new issue code, please also
 * add a new optional field to this type.
 */
public data class InspectorIssueDetails(
  @JsonProperty("sameSiteCookieIssueDetails")
  @Optional
  public val sameSiteCookieIssueDetails: SameSiteCookieIssueDetails? = null,
  @JsonProperty("mixedContentIssueDetails")
  @Optional
  public val mixedContentIssueDetails: MixedContentIssueDetails? = null,
  @JsonProperty("blockedByResponseIssueDetails")
  @Optional
  public val blockedByResponseIssueDetails: BlockedByResponseIssueDetails? = null,
  @JsonProperty("heavyAdIssueDetails")
  @Optional
  public val heavyAdIssueDetails: HeavyAdIssueDetails? = null,
  @JsonProperty("contentSecurityPolicyIssueDetails")
  @Optional
  public val contentSecurityPolicyIssueDetails: ContentSecurityPolicyIssueDetails? = null,
  @JsonProperty("sharedArrayBufferIssueDetails")
  @Optional
  public val sharedArrayBufferIssueDetails: SharedArrayBufferIssueDetails? = null,
  @JsonProperty("twaQualityEnforcementDetails")
  @Optional
  public val twaQualityEnforcementDetails: TrustedWebActivityIssueDetails? = null,
  @JsonProperty("lowTextContrastIssueDetails")
  @Optional
  public val lowTextContrastIssueDetails: LowTextContrastIssueDetails? = null,
  @JsonProperty("corsIssueDetails")
  @Optional
  public val corsIssueDetails: CorsIssueDetails? = null,
  @JsonProperty("attributionReportingIssueDetails")
  @Optional
  public val attributionReportingIssueDetails: AttributionReportingIssueDetails? = null,
)
