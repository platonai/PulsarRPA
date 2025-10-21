package ai.platon.cdt.kt.protocol.events.webaudio

import ai.platon.cdt.kt.protocol.types.webaudio.BaseAudioContext
import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Notifies that existing BaseAudioContext has changed some properties (id stays the same)..
 */
public data class ContextChanged(
  @JsonProperty("context")
  public val context: BaseAudioContext,
)
