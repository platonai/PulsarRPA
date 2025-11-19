@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.dom

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int
import kotlin.String

/**
 * Mirrors `DOMCharacterDataModified` event.
 */
data class CharacterDataModified(
  @param:JsonProperty("nodeId")
  val nodeId: Int,
  @param:JsonProperty("characterData")
  val characterData: String,
)
