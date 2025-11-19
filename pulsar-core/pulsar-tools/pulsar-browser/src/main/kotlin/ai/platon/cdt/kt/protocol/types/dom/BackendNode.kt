@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.dom

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int
import kotlin.String

/**
 * Backend node with a friendly name.
 */
data class BackendNode(
  @param:JsonProperty("nodeType")
  val nodeType: Int,
  @param:JsonProperty("nodeName")
  val nodeName: String,
  @param:JsonProperty("backendNodeId")
  val backendNodeId: Int,
)
