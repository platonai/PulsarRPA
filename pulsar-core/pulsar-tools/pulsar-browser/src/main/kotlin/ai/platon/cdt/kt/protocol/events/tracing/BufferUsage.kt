package ai.platon.cdt.kt.protocol.events.tracing

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double

public data class BufferUsage(
  @JsonProperty("percentFull")
  @Optional
  public val percentFull: Double? = null,
  @JsonProperty("eventCount")
  @Optional
  public val eventCount: Double? = null,
  @JsonProperty("value")
  @Optional
  public val `value`: Double? = null,
)
