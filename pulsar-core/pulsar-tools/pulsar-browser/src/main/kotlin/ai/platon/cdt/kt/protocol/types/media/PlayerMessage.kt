@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.media

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * Have one type per entry in MediaLogRecord::Type
 * Corresponds to kMessage
 */
data class PlayerMessage(
  @param:JsonProperty("level")
  val level: PlayerMessageLevel,
  @param:JsonProperty("message")
  val message: String,
)
