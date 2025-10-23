@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.network

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * The reason why request was blocked.
 */
public enum class BlockedReason {
  @JsonProperty("other")
  OTHER,
  @JsonProperty("csp")
  CSP,
  @JsonProperty("mixed-content")
  MIXED_CONTENT,
  @JsonProperty("origin")
  ORIGIN,
  @JsonProperty("inspector")
  INSPECTOR,
  @JsonProperty("subresource-filter")
  SUBRESOURCE_FILTER,
  @JsonProperty("content-type")
  CONTENT_TYPE,
  @JsonProperty("coep-frame-resource-needs-coep-header")
  COEP_FRAME_RESOURCE_NEEDS_COEP_HEADER,
  @JsonProperty("coop-sandboxed-iframe-cannot-navigate-to-coop-page")
  COOP_SANDBOXED_IFRAME_CANNOT_NAVIGATE_TO_COOP_PAGE,
  @JsonProperty("corp-not-same-origin")
  CORP_NOT_SAME_ORIGIN,
  @JsonProperty("corp-not-same-origin-after-defaulted-to-same-origin-by-coep")
  CORP_NOT_SAME_ORIGIN_AFTER_DEFAULTED_TO_SAME_ORIGIN_BY_COEP,
  @JsonProperty("corp-not-same-site")
  CORP_NOT_SAME_SITE,
}
