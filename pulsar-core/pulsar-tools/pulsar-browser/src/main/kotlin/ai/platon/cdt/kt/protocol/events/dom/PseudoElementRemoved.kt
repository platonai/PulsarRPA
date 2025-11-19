@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.dom

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int

/**
 * Called when a pseudo element is removed from an element.
 */
@Experimental
data class PseudoElementRemoved(
  @param:JsonProperty("parentId")
  val parentId: Int,
  @param:JsonProperty("pseudoElementId")
  val pseudoElementId: Int,
)
