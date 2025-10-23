@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.layertree

import ai.platon.cdt.kt.protocol.types.dom.Rect
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

data class LayerPainted(
  @param:JsonProperty("layerId")
  val layerId: String,
  @param:JsonProperty("clip")
  val clip: Rect,
)
