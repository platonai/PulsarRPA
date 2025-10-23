@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.dom

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int

data class FrameOwner(
  @param:JsonProperty("backendNodeId")
  val backendNodeId: Int,
  @param:JsonProperty("nodeId")
  @param:Optional
  val nodeId: Int? = null,
)
