@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.audits

import com.fasterxml.jackson.`annotation`.JsonProperty

public enum class MixedContentResolutionStatus {
  @JsonProperty("MixedContentBlocked")
  MIXED_CONTENT_BLOCKED,
  @JsonProperty("MixedContentAutomaticallyUpgraded")
  MIXED_CONTENT_AUTOMATICALLY_UPGRADED,
  @JsonProperty("MixedContentWarning")
  MIXED_CONTENT_WARNING,
}
