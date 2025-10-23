@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.headlessexperimental

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Image compression format (defaults to png).
 */
public enum class ScreenshotParamsFormat {
  @JsonProperty("jpeg")
  JPEG,
  @JsonProperty("png")
  PNG,
}
