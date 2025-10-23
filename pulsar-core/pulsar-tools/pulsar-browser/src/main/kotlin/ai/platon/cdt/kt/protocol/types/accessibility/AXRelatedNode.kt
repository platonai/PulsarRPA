@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.accessibility

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int
import kotlin.String

data class AXRelatedNode(
  @param:JsonProperty("backendDOMNodeId")
  val backendDOMNodeId: Int,
  @param:JsonProperty("idref")
  @param:Optional
  val idref: String? = null,
  @param:JsonProperty("text")
  @param:Optional
  val text: String? = null,
)
