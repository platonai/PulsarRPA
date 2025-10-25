@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.browser

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Whether to allow all or deny all download requests, or use default Chrome behavior if
 * available (otherwise deny). |allowAndName| allows download and names files according to
 * their dowmload guids.
 */
public enum class SetDownloadBehaviorBehavior {
  @JsonProperty("deny")
  DENY,
  @JsonProperty("allow")
  ALLOW,
  @JsonProperty("allowAndName")
  ALLOW_AND_NAME,
  @JsonProperty("default")
  DEFAULT,
}
