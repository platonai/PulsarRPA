package ai.platon.cdt.kt.protocol.types.animation

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * Keyframe Style
 */
public data class KeyframeStyle(
  @JsonProperty("offset")
  public val offset: String,
  @JsonProperty("easing")
  public val easing: String,
)
