@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.debugger

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String
import kotlin.collections.List

data class SetBreakpointByUrl(
  @param:JsonProperty("breakpointId")
  val breakpointId: String,
  @param:JsonProperty("locations")
  val locations: List<Location>,
)
