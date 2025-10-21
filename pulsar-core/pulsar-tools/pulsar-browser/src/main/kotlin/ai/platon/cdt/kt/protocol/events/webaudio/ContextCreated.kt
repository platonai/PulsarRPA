package ai.platon.cdt.kt.protocol.events.webaudio

import ai.platon.cdt.kt.protocol.types.webaudio.BaseAudioContext
import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Notifies that a new BaseAudioContext has been created.
 */
public data class ContextCreated(
  @JsonProperty("context")
  public val context: BaseAudioContext,
)
