@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.dom

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int
import kotlin.collections.List

/**
 * Fired when `Element`'s inline style is modified via a CSS property modification.
 */
@Experimental
data class InlineStyleInvalidated(
  @param:JsonProperty("nodeIds")
  val nodeIds: List<Int>,
)
