@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.debugger

import ai.platon.cdt.kt.protocol.types.debugger.Location
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * Fired when breakpoint is resolved to an actual script and location.
 */
data class BreakpointResolved(
  @param:JsonProperty("breakpointId")
  val breakpointId: String,
  @param:JsonProperty("location")
  val location: Location,
)
