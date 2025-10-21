package ai.platon.cdt.kt.protocol.types.performancetimeline

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.String

public data class TimelineEvent(
  @JsonProperty("frameId")
  public val frameId: String,
  @JsonProperty("type")
  public val type: String,
  @JsonProperty("name")
  public val name: String,
  @JsonProperty("time")
  public val time: Double,
  @JsonProperty("duration")
  @Optional
  public val duration: Double? = null,
  @JsonProperty("lcpDetails")
  @Optional
  public val lcpDetails: LargestContentfulPaint? = null,
  @JsonProperty("layoutShiftDetails")
  @Optional
  public val layoutShiftDetails: LayoutShift? = null,
)
