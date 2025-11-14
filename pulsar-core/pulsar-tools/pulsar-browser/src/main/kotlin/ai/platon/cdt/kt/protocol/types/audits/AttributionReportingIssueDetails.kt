@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.audits

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int
import kotlin.String

/**
 * Details for issues around "Attribution Reporting API" usage.
 * Explainer: https://github.com/WICG/conversion-measurement-api
 */
data class AttributionReportingIssueDetails(
  @param:JsonProperty("violationType")
  val violationType: AttributionReportingIssueType,
  @param:JsonProperty("frame")
  @param:Optional
  val frame: AffectedFrame? = null,
  @param:JsonProperty("request")
  @param:Optional
  val request: AffectedRequest? = null,
  @param:JsonProperty("violatingNodeId")
  @param:Optional
  val violatingNodeId: Int? = null,
  @param:JsonProperty("invalidParameter")
  @param:Optional
  val invalidParameter: String? = null,
)
