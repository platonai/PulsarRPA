@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.media

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * Corresponds to kMediaPropertyChange
 */
data class PlayerProperty(
  @param:JsonProperty("name")
  val name: String,
  @param:JsonProperty("value")
  val `value`: String,
)
