@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.dom

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int
import kotlin.String

/**
 * Fired when `Element`'s attribute is modified.
 */
data class AttributeModified(
  @param:JsonProperty("nodeId")
  val nodeId: Int,
  @param:JsonProperty("name")
  val name: String,
  @param:JsonProperty("value")
  val `value`: String,
)
