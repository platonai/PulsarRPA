package ai.platon.cdt.kt.protocol.types.audits

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Details for a request that has been blocked with the BLOCKED_BY_RESPONSE
 * code. Currently only used for COEP/COOP, but may be extended to include
 * some CSP errors in the future.
 */
public data class BlockedByResponseIssueDetails(
  @JsonProperty("request")
  public val request: AffectedRequest,
  @JsonProperty("parentFrame")
  @Optional
  public val parentFrame: AffectedFrame? = null,
  @JsonProperty("blockedFrame")
  @Optional
  public val blockedFrame: AffectedFrame? = null,
  @JsonProperty("reason")
  public val reason: BlockedByResponseReason,
)
