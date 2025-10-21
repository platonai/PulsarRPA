package ai.platon.cdt.kt.protocol.types.page

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Reason for a permissions policy feature to be disabled.
 */
public enum class PermissionsPolicyBlockReason {
  @JsonProperty("Header")
  HEADER,
  @JsonProperty("IframeAttribute")
  IFRAME_ATTRIBUTE,
}
