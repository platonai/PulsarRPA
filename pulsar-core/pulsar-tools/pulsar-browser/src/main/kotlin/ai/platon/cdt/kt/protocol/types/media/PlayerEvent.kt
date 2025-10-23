@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.media

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.String

/**
 * Corresponds to kMediaEventTriggered
 */
data class PlayerEvent(
  @param:JsonProperty("timestamp")
  val timestamp: Double,
  @param:JsonProperty("value")
  val `value`: String,
)
