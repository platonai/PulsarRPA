package ai.platon.cdt.kt.protocol.types.audits

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * An inspector issue reported from the back-end.
 */
public data class InspectorIssue(
  @JsonProperty("code")
  public val code: InspectorIssueCode,
  @JsonProperty("details")
  public val details: InspectorIssueDetails,
)
