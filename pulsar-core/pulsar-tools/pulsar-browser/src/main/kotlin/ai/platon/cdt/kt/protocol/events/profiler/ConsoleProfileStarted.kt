package ai.platon.cdt.kt.protocol.events.profiler

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.types.debugger.Location
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * Sent when new profile recording is started using console.profile() call.
 */
public data class ConsoleProfileStarted(
  @JsonProperty("id")
  public val id: String,
  @JsonProperty("location")
  public val location: Location,
  @JsonProperty("title")
  @Optional
  public val title: String? = null,
)
