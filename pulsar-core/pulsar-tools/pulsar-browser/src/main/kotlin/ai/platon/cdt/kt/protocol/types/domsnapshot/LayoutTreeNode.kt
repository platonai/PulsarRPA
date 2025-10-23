@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.domsnapshot

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.types.dom.Rect
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.Int
import kotlin.String
import kotlin.collections.List

/**
 * Details of an element in the DOM tree with a LayoutObject.
 */
data class LayoutTreeNode(
  @param:JsonProperty("domNodeIndex")
  val domNodeIndex: Int,
  @param:JsonProperty("boundingBox")
  val boundingBox: Rect,
  @param:JsonProperty("layoutText")
  @param:Optional
  val layoutText: String? = null,
  @param:JsonProperty("inlineTextNodes")
  @param:Optional
  val inlineTextNodes: List<InlineTextBox>? = null,
  @param:JsonProperty("styleIndex")
  @param:Optional
  val styleIndex: Int? = null,
  @param:JsonProperty("paintOrder")
  @param:Optional
  val paintOrder: Int? = null,
  @param:JsonProperty("isStackingContext")
  @param:Optional
  val isStackingContext: Boolean? = null,
)
