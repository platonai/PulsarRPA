package ai.platon.cdt.kt.protocol.types.media

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * Corresponds to kMediaError
 */
public data class PlayerError(
  @JsonProperty("type")
  public val type: PlayerErrorType,
  @JsonProperty("errorCode")
  public val errorCode: String,
)
