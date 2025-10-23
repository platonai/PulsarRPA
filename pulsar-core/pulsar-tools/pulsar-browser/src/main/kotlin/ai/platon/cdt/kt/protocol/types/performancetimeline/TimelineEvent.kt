@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.performancetimeline

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.String

data class TimelineEvent(
  @param:JsonProperty("frameId")
  val frameId: String,
  @param:JsonProperty("type")
  val type: String,
  @param:JsonProperty("name")
  val name: String,
  @param:JsonProperty("time")
  val time: Double,
  @param:JsonProperty("duration")
  @param:Optional
  val duration: Double? = null,
  @param:JsonProperty("lcpDetails")
  @param:Optional
  val lcpDetails: LargestContentfulPaint? = null,
  @param:JsonProperty("layoutShiftDetails")
  @param:Optional
  val layoutShiftDetails: LayoutShift? = null,
)
