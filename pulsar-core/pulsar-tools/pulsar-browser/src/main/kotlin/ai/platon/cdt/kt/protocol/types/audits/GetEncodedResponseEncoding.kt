@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.audits

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * The encoding to use.
 */
public enum class GetEncodedResponseEncoding {
  @JsonProperty("webp")
  WEBP,
  @JsonProperty("jpeg")
  JPEG,
  @JsonProperty("png")
  PNG,
}
