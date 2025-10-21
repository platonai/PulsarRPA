package ai.platon.cdt.kt.protocol.types.performancetimeline

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.Double
import kotlin.collections.List

/**
 * See https://wicg.github.io/layout-instability/#sec-layout-shift and layout_shift.idl
 */
public data class LayoutShift(
  @JsonProperty("value")
  public val `value`: Double,
  @JsonProperty("hadRecentInput")
  public val hadRecentInput: Boolean,
  @JsonProperty("lastInputTime")
  public val lastInputTime: Double,
  @JsonProperty("sources")
  public val sources: List<LayoutShiftAttribution>,
)
