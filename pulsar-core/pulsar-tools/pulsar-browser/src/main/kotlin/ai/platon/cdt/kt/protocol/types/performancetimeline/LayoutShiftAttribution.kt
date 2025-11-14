@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.performancetimeline

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.types.dom.Rect
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int

data class LayoutShiftAttribution(
  @param:JsonProperty("previousRect")
  val previousRect: Rect,
  @param:JsonProperty("currentRect")
  val currentRect: Rect,
  @param:JsonProperty("nodeId")
  @param:Optional
  val nodeId: Int? = null,
)
