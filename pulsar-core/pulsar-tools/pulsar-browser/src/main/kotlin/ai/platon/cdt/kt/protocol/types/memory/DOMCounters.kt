@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.memory

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int

data class DOMCounters(
  @param:JsonProperty("documents")
  val documents: Int,
  @param:JsonProperty("nodes")
  val nodes: Int,
  @param:JsonProperty("jsEventListeners")
  val jsEventListeners: Int,
)
