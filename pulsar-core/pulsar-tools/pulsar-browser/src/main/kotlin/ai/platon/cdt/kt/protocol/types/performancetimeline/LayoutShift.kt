@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.performancetimeline

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.Double
import kotlin.collections.List

/**
 * See https://wicg.github.io/layout-instability/#sec-layout-shift and layout_shift.idl
 */
data class LayoutShift(
  @param:JsonProperty("value")
  val `value`: Double,
  @param:JsonProperty("hadRecentInput")
  val hadRecentInput: Boolean,
  @param:JsonProperty("lastInputTime")
  val lastInputTime: Double,
  @param:JsonProperty("sources")
  val sources: List<LayoutShiftAttribution>,
)
