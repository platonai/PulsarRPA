@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.browser

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Browser command ids used by executeBrowserCommand.
 */
public enum class BrowserCommandId {
  @JsonProperty("openTabSearch")
  OPEN_TAB_SEARCH,
  @JsonProperty("closeTabSearch")
  CLOSE_TAB_SEARCH,
}
