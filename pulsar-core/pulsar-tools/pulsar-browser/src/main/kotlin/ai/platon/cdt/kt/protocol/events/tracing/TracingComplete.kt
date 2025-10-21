package ai.platon.cdt.kt.protocol.events.tracing

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.types.tracing.StreamCompression
import ai.platon.cdt.kt.protocol.types.tracing.StreamFormat
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.String

/**
 * Signals that tracing is stopped and there is no trace buffers pending flush, all data were
 * delivered via dataCollected events.
 */
public data class TracingComplete(
  @JsonProperty("dataLossOccurred")
  public val dataLossOccurred: Boolean,
  @JsonProperty("stream")
  @Optional
  public val stream: String? = null,
  @JsonProperty("traceFormat")
  @Optional
  public val traceFormat: StreamFormat? = null,
  @JsonProperty("streamCompression")
  @Optional
  public val streamCompression: StreamCompression? = null,
)
