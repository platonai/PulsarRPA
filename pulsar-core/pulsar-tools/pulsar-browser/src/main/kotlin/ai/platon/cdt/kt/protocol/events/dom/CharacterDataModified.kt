package ai.platon.cdt.kt.protocol.events.dom

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int
import kotlin.String

/**
 * Mirrors `DOMCharacterDataModified` event.
 */
public data class CharacterDataModified(
  @JsonProperty("nodeId")
  public val nodeId: Int,
  @JsonProperty("characterData")
  public val characterData: String,
)
