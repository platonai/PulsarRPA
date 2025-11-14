@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.media

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * Corresponds to kMediaError
 */
data class PlayerError(
  @param:JsonProperty("type")
  val type: PlayerErrorType,
  @param:JsonProperty("errorCode")
  val errorCode: String,
)
