package ai.platon.cdt.kt.protocol.events.debugger

import ai.platon.cdt.kt.protocol.types.debugger.Location
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * Fired when breakpoint is resolved to an actual script and location.
 */
public data class BreakpointResolved(
  @JsonProperty("breakpointId")
  public val breakpointId: String,
  @JsonProperty("location")
  public val location: Location,
)
