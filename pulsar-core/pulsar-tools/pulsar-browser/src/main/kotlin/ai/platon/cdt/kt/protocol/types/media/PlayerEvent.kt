package ai.platon.cdt.kt.protocol.types.media

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.String

/**
 * Corresponds to kMediaEventTriggered
 */
public data class PlayerEvent(
  @JsonProperty("timestamp")
  public val timestamp: Double,
  @JsonProperty("value")
  public val `value`: String,
)
