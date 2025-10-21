package ai.platon.cdt.kt.protocol.types.systeminfo

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Image format of a given image.
 */
public enum class ImageType {
  @JsonProperty("jpeg")
  JPEG,
  @JsonProperty("webp")
  WEBP,
  @JsonProperty("unknown")
  UNKNOWN,
}
