@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.accessibility

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Any
import kotlin.collections.List

/**
 * A single computed AX property.
 */
data class AXValue(
  @param:JsonProperty("type")
  val type: AXValueType,
  @param:JsonProperty("value")
  @param:Optional
  val `value`: Any? = null,
  @param:JsonProperty("relatedNodes")
  @param:Optional
  val relatedNodes: List<AXRelatedNode>? = null,
  @param:JsonProperty("sources")
  @param:Optional
  val sources: List<AXValueSource>? = null,
)
