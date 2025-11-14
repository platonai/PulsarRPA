@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.audits

import com.fasterxml.jackson.`annotation`.JsonProperty

public enum class AttributionReportingIssueType {
  @JsonProperty("PermissionPolicyDisabled")
  PERMISSION_POLICY_DISABLED,
  @JsonProperty("InvalidAttributionSourceEventId")
  INVALID_ATTRIBUTION_SOURCE_EVENT_ID,
  @JsonProperty("InvalidAttributionData")
  INVALID_ATTRIBUTION_DATA,
  @JsonProperty("AttributionSourceUntrustworthyOrigin")
  ATTRIBUTION_SOURCE_UNTRUSTWORTHY_ORIGIN,
  @JsonProperty("AttributionUntrustworthyOrigin")
  ATTRIBUTION_UNTRUSTWORTHY_ORIGIN,
}
