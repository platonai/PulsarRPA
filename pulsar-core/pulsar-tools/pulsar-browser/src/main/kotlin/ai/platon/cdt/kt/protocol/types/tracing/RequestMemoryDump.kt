@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.tracing

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.String

data class RequestMemoryDump(
  @param:JsonProperty("dumpGuid")
  val dumpGuid: String,
  @param:JsonProperty("success")
  val success: Boolean,
)
