@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.dom

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int
import kotlin.String

data class NodeForLocation(
  @param:JsonProperty("backendNodeId")
  val backendNodeId: Int,
  @param:JsonProperty("frameId")
  val frameId: String,
  @param:JsonProperty("nodeId")
  @param:Optional
  val nodeId: Int? = null,
)
