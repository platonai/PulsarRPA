package ai.platon.cdt.kt.protocol.events.audits

import ai.platon.cdt.kt.protocol.types.audits.InspectorIssue
import com.fasterxml.jackson.`annotation`.JsonProperty

public data class IssueAdded(
  @JsonProperty("issue")
  public val issue: InspectorIssue,
)
