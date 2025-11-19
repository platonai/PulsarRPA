@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.animation

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * Event for each animation that has been created.
 */
data class AnimationCreated(
  @param:JsonProperty("id")
  val id: String,
)
