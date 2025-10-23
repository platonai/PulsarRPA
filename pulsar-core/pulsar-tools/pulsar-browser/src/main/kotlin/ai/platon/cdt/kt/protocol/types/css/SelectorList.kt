@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.css

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String
import kotlin.collections.List

/**
 * Selector list data.
 */
data class SelectorList(
  @param:JsonProperty("selectors")
  val selectors: List<Value>,
  @param:JsonProperty("text")
  val text: String,
)
