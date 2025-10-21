package ai.platon.cdt.kt.protocol.events.page

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.String
import kotlin.collections.List

/**
 * Fired when a new window is going to be opened, via window.open(), link click, form submission,
 * etc.
 */
public data class WindowOpen(
  @JsonProperty("url")
  public val url: String,
  @JsonProperty("windowName")
  public val windowName: String,
  @JsonProperty("windowFeatures")
  public val windowFeatures: List<String>,
  @JsonProperty("userGesture")
  public val userGesture: Boolean,
)
