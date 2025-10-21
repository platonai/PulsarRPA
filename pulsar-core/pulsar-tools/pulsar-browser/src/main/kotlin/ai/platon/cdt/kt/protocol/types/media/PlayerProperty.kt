package ai.platon.cdt.kt.protocol.types.media

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * Corresponds to kMediaPropertyChange
 */
public data class PlayerProperty(
  @JsonProperty("name")
  public val name: String,
  @JsonProperty("value")
  public val `value`: String,
)
