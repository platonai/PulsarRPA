package ai.platon.cdt.kt.protocol.types.page

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Whether to allow all or deny all download requests, or use default Chrome behavior if
 * available (otherwise deny).
 */
public enum class SetDownloadBehaviorBehavior {
  @JsonProperty("deny")
  DENY,
  @JsonProperty("allow")
  ALLOW,
  @JsonProperty("default")
  DEFAULT,
}
