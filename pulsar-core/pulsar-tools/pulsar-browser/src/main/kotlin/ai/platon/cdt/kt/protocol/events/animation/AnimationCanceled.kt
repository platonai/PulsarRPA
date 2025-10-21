package ai.platon.cdt.kt.protocol.events.animation

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * Event for when an animation has been cancelled.
 */
public data class AnimationCanceled(
  @JsonProperty("id")
  public val id: String,
)
