@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.audits

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * This struct holds a list of optional fields with additional information
 * specific to the kind of issue. When adding a new issue code, please also
 * add a new optional field to this type.
 */
data class InspectorIssueDetails(
  @param:JsonProperty("sameSiteCookieIssueDetails")
  @param:Optional
  val sameSiteCookieIssueDetails: SameSiteCookieIssueDetails? = null,
  @param:JsonProperty("mixedContentIssueDetails")
  @param:Optional
  val mixedContentIssueDetails: MixedContentIssueDetails? = null,
  @param:JsonProperty("blockedByResponseIssueDetails")
  @param:Optional
  val blockedByResponseIssueDetails: BlockedByResponseIssueDetails? = null,
  @param:JsonProperty("heavyAdIssueDetails")
  @param:Optional
  val heavyAdIssueDetails: HeavyAdIssueDetails? = null,
  @param:JsonProperty("contentSecurityPolicyIssueDetails")
  @param:Optional
  val contentSecurityPolicyIssueDetails: ContentSecurityPolicyIssueDetails? = null,
  @param:JsonProperty("sharedArrayBufferIssueDetails")
  @param:Optional
  val sharedArrayBufferIssueDetails: SharedArrayBufferIssueDetails? = null,
  @param:JsonProperty("twaQualityEnforcementDetails")
  @param:Optional
  val twaQualityEnforcementDetails: TrustedWebActivityIssueDetails? = null,
  @param:JsonProperty("lowTextContrastIssueDetails")
  @param:Optional
  val lowTextContrastIssueDetails: LowTextContrastIssueDetails? = null,
  @param:JsonProperty("corsIssueDetails")
  @param:Optional
  val corsIssueDetails: CorsIssueDetails? = null,
  @param:JsonProperty("attributionReportingIssueDetails")
  @param:Optional
  val attributionReportingIssueDetails: AttributionReportingIssueDetails? = null,
)
