@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.media

import com.fasterxml.jackson.`annotation`.JsonProperty

public enum class PlayerErrorType {
  @JsonProperty("pipeline_error")
  PIPELINE_ERROR,
  @JsonProperty("media_error")
  MEDIA_ERROR,
}
