package ai.platon.cdt.kt.protocol.types.debugger

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String
import kotlin.collections.List

public data class SetBreakpointByUrl(
  @JsonProperty("breakpointId")
  public val breakpointId: String,
  @JsonProperty("locations")
  public val locations: List<Location>,
)
