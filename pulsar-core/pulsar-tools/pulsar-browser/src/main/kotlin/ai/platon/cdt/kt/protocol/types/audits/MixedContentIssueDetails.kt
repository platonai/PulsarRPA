@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.audits

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

data class MixedContentIssueDetails(
  @param:JsonProperty("resourceType")
  @param:Optional
  val resourceType: MixedContentResourceType? = null,
  @param:JsonProperty("resolutionStatus")
  val resolutionStatus: MixedContentResolutionStatus,
  @param:JsonProperty("insecureURL")
  val insecureURL: String,
  @param:JsonProperty("mainResourceURL")
  val mainResourceURL: String,
  @param:JsonProperty("request")
  @param:Optional
  val request: AffectedRequest? = null,
  @param:JsonProperty("frame")
  @param:Optional
  val frame: AffectedFrame? = null,
)
