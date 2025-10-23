@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.animation

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * Event for when an animation has been cancelled.
 */
data class AnimationCanceled(
  @param:JsonProperty("id")
  val id: String,
)
