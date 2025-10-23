@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.layertree

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.types.layertree.Layer
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.collections.List

data class LayerTreeDidChange(
  @param:JsonProperty("layers")
  @param:Optional
  val layers: List<Layer>? = null,
)
