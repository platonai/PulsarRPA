package ai.platon.cdt.kt.protocol.types.css

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.collections.List

/**
 * CSS keyframes rule representation.
 */
public data class CSSKeyframesRule(
  @JsonProperty("animationName")
  public val animationName: Value,
  @JsonProperty("keyframes")
  public val keyframes: List<CSSKeyframeRule>,
)
