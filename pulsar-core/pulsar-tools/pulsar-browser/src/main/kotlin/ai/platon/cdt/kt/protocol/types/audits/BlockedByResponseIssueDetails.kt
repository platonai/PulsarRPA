@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.audits

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Details for a request that has been blocked with the BLOCKED_BY_RESPONSE
 * code. Currently only used for COEP/COOP, but may be extended to include
 * some CSP errors in the future.
 */
data class BlockedByResponseIssueDetails(
  @param:JsonProperty("request")
  val request: AffectedRequest,
  @param:JsonProperty("parentFrame")
  @param:Optional
  val parentFrame: AffectedFrame? = null,
  @param:JsonProperty("blockedFrame")
  @param:Optional
  val blockedFrame: AffectedFrame? = null,
  @param:JsonProperty("reason")
  val reason: BlockedByResponseReason,
)
