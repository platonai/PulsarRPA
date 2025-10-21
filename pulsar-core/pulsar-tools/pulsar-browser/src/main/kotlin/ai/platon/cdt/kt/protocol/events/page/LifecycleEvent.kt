package ai.platon.cdt.kt.protocol.events.page

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.String

/**
 * Fired for top level page lifecycle events such as navigation, load, paint, etc.
 */
public data class LifecycleEvent(
  @JsonProperty("frameId")
  public val frameId: String,
  @JsonProperty("loaderId")
  public val loaderId: String,
  @JsonProperty("name")
  public val name: String,
  @JsonProperty("timestamp")
  public val timestamp: Double,
)
