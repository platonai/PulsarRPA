@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.network

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * List of content encodings supported by the backend.
 */
public enum class ContentEncoding {
  @JsonProperty("deflate")
  DEFLATE,
  @JsonProperty("gzip")
  GZIP,
  @JsonProperty("br")
  BR,
}
