@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.dom

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.types.dom.Node
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int

/**
 * Called when a pseudo element is added to an element.
 */
@Experimental
data class PseudoElementAdded(
  @param:JsonProperty("parentId")
  val parentId: Int,
  @param:JsonProperty("pseudoElement")
  val pseudoElement: Node,
)
