@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.audits

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Enum indicating the reason a response has been blocked. These reasons are
 * refinements of the net error BLOCKED_BY_RESPONSE.
 */
public enum class BlockedByResponseReason {
  @JsonProperty("CoepFrameResourceNeedsCoepHeader")
  COEP_FRAME_RESOURCE_NEEDS_COEP_HEADER,
  @JsonProperty("CoopSandboxedIFrameCannotNavigateToCoopPage")
  COOP_SANDBOXED_I_FRAME_CANNOT_NAVIGATE_TO_COOP_PAGE,
  @JsonProperty("CorpNotSameOrigin")
  CORP_NOT_SAME_ORIGIN,
  @JsonProperty("CorpNotSameOriginAfterDefaultedToSameOriginByCoep")
  CORP_NOT_SAME_ORIGIN_AFTER_DEFAULTED_TO_SAME_ORIGIN_BY_COEP,
  @JsonProperty("CorpNotSameSite")
  CORP_NOT_SAME_SITE,
}
