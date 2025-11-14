@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.emulation

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Enum of image types that can be disabled.
 */
public enum class DisabledImageType {
  @JsonProperty("avif")
  AVIF,
  @JsonProperty("jxl")
  JXL,
  @JsonProperty("webp")
  WEBP,
}
