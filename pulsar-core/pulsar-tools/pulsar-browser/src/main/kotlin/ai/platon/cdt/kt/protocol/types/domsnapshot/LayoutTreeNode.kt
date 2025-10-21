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
public data class LayoutTreeNode(
  @JsonProperty("domNodeIndex")
  public val domNodeIndex: Int,
  @JsonProperty("boundingBox")
  public val boundingBox: Rect,
  @JsonProperty("layoutText")
  @Optional
  public val layoutText: String? = null,
  @JsonProperty("inlineTextNodes")
  @Optional
  public val inlineTextNodes: List<InlineTextBox>? = null,
  @JsonProperty("styleIndex")
  @Optional
  public val styleIndex: Int? = null,
  @JsonProperty("paintOrder")
  @Optional
  public val paintOrder: Int? = null,
  @JsonProperty("isStackingContext")
  @Optional
  public val isStackingContext: Boolean? = null,
)
