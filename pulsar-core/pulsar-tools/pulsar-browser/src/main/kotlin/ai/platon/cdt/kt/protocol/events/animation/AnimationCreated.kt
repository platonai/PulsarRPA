package ai.platon.cdt.kt.protocol.events.animation

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * Event for each animation that has been created.
 */
public data class AnimationCreated(
  @JsonProperty("id")
  public val id: String,
)
