@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.animation

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.Int
import kotlin.String

/**
 * AnimationEffect instance
 */
data class AnimationEffect(
  @param:JsonProperty("delay")
  val delay: Double,
  @param:JsonProperty("endDelay")
  val endDelay: Double,
  @param:JsonProperty("iterationStart")
  val iterationStart: Double,
  @param:JsonProperty("iterations")
  val iterations: Double,
  @param:JsonProperty("duration")
  val duration: Double,
  @param:JsonProperty("direction")
  val direction: String,
  @param:JsonProperty("fill")
  val fill: String,
  @param:JsonProperty("backendNodeId")
  @param:Optional
  val backendNodeId: Int? = null,
  @param:JsonProperty("keyframesRule")
  @param:Optional
  val keyframesRule: KeyframesRule? = null,
  @param:JsonProperty("easing")
  val easing: String,
)
