@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.debugger

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int
import kotlin.String

data class BreakLocation(
  @param:JsonProperty("scriptId")
  val scriptId: String,
  @param:JsonProperty("lineNumber")
  val lineNumber: Int,
  @param:JsonProperty("columnNumber")
  @param:Optional
  val columnNumber: Int? = null,
  @param:JsonProperty("type")
  @param:Optional
  val type: BreakLocationType? = null,
)
