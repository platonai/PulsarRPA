@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.domsnapshot

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.collections.List

data class Snapshot(
  @param:JsonProperty("domNodes")
  val domNodes: List<DOMNode>,
  @param:JsonProperty("layoutTreeNodes")
  val layoutTreeNodes: List<LayoutTreeNode>,
  @param:JsonProperty("computedStyles")
  val computedStyles: List<ComputedStyle>,
)
