@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.accessibility

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.Int
import kotlin.String
import kotlin.collections.List

/**
 * A node in the accessibility tree.
 */
data class AXNode(
  @param:JsonProperty("nodeId")
  val nodeId: String,
  @param:JsonProperty("ignored")
  val ignored: Boolean,
  @param:JsonProperty("ignoredReasons")
  @param:Optional
  val ignoredReasons: List<AXProperty>? = null,
  @param:JsonProperty("role")
  @param:Optional
  val role: AXValue? = null,
  @param:JsonProperty("name")
  @param:Optional
  val name: AXValue? = null,
  @param:JsonProperty("description")
  @param:Optional
  val description: AXValue? = null,
  @param:JsonProperty("value")
  @param:Optional
  val `value`: AXValue? = null,
  @param:JsonProperty("properties")
  @param:Optional
  val properties: List<AXProperty>? = null,
  @param:JsonProperty("childIds")
  @param:Optional
  val childIds: List<String>? = null,
  @param:JsonProperty("backendDOMNodeId")
  @param:Optional
  val backendDOMNodeId: Int? = null,
)
