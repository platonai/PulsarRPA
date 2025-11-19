@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.animation

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * Keyframe Style
 */
data class KeyframeStyle(
  @param:JsonProperty("offset")
  val offset: String,
  @param:JsonProperty("easing")
  val easing: String,
)
