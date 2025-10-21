package ai.platon.cdt.kt.protocol.events.media

import ai.platon.cdt.kt.protocol.types.media.PlayerProperty
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String
import kotlin.collections.List

/**
 * This can be called multiple times, and can be used to set / override /
 * remove player properties. A null propValue indicates removal.
 */
public data class PlayerPropertiesChanged(
  @JsonProperty("playerId")
  public val playerId: String,
  @JsonProperty("properties")
  public val properties: List<PlayerProperty>,
)
