@file:Suppress("unused")
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
data class TracingComplete(
  @param:JsonProperty("dataLossOccurred")
  val dataLossOccurred: Boolean,
  @param:JsonProperty("stream")
  @param:Optional
  val stream: String? = null,
  @param:JsonProperty("traceFormat")
  @param:Optional
  val traceFormat: StreamFormat? = null,
  @param:JsonProperty("streamCompression")
  @param:Optional
  val streamCompression: StreamCompression? = null,
)
