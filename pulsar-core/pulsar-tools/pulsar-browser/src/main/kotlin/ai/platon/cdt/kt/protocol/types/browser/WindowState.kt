@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.browser

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * The state of the browser window.
 */
public enum class WindowState {
  @JsonProperty("normal")
  NORMAL,
  @JsonProperty("minimized")
  MINIMIZED,
  @JsonProperty("maximized")
  MAXIMIZED,
  @JsonProperty("fullscreen")
  FULLSCREEN,
}
