package ai.platon.cdt.kt.protocol.events.target

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * Issued when a target is destroyed.
 */
public data class TargetDestroyed(
  @JsonProperty("targetId")
  public val targetId: String,
)
