package ai.platon.cdt.kt.protocol.types.animation

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String
import kotlin.collections.List

/**
 * Keyframes Rule
 */
public data class KeyframesRule(
  @JsonProperty("name")
  @Optional
  public val name: String? = null,
  @JsonProperty("keyframes")
  public val keyframes: List<KeyframeStyle>,
)
