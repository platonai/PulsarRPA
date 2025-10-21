package ai.platon.cdt.kt.protocol.types.domsnapshot

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.collections.List

public data class Snapshot(
  @JsonProperty("domNodes")
  public val domNodes: List<DOMNode>,
  @JsonProperty("layoutTreeNodes")
  public val layoutTreeNodes: List<LayoutTreeNode>,
  @JsonProperty("computedStyles")
  public val computedStyles: List<ComputedStyle>,
)
