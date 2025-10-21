package ai.platon.cdt.kt.protocol.events.webaudio

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * Notifies that an existing AudioNode has been destroyed.
 */
public data class AudioNodeWillBeDestroyed(
  @JsonProperty("contextId")
  public val contextId: String,
  @JsonProperty("nodeId")
  public val nodeId: String,
)
