@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.audits

import com.fasterxml.jackson.`annotation`.JsonProperty

data class HeavyAdIssueDetails(
  @param:JsonProperty("resolution")
  val resolution: HeavyAdResolutionStatus,
  @param:JsonProperty("reason")
  val reason: HeavyAdReason,
  @param:JsonProperty("frame")
  val frame: AffectedFrame,
)
