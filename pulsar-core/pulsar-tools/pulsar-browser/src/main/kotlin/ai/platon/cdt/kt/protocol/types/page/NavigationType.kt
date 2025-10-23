@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.page

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * The type of a frameNavigated event.
 */
public enum class NavigationType {
  @JsonProperty("Navigation")
  NAVIGATION,
  @JsonProperty("BackForwardCacheRestore")
  BACK_FORWARD_CACHE_RESTORE,
}
