package ai.platon.cdt.kt.protocol.events.overlay

import ai.platon.cdt.kt.protocol.types.page.Viewport
import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Fired when user asks to capture screenshot of some area on the page.
 */
public data class ScreenshotRequested(
  @JsonProperty("viewport")
  public val viewport: Viewport,
)
