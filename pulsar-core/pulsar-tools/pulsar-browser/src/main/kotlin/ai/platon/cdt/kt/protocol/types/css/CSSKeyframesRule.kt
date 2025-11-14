@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.css

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.collections.List

/**
 * CSS keyframes rule representation.
 */
data class CSSKeyframesRule(
  @param:JsonProperty("animationName")
  val animationName: Value,
  @param:JsonProperty("keyframes")
  val keyframes: List<CSSKeyframeRule>,
)
