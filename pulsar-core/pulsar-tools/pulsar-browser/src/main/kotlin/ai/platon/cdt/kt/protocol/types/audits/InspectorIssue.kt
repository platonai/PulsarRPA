@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.audits

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * An inspector issue reported from the back-end.
 */
data class InspectorIssue(
  @param:JsonProperty("code")
  val code: InspectorIssueCode,
  @param:JsonProperty("details")
  val details: InspectorIssueDetails,
)
