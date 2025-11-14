@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.tracing

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double

data class BufferUsage(
  @param:JsonProperty("percentFull")
  @param:Optional
  val percentFull: Double? = null,
  @param:JsonProperty("eventCount")
  @param:Optional
  val eventCount: Double? = null,
  @param:JsonProperty("value")
  @param:Optional
  val `value`: Double? = null,
)
