@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.emulation

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int

/**
 * Screen orientation.
 */
data class ScreenOrientation(
  @param:JsonProperty("type")
  val type: ScreenOrientationType,
  @param:JsonProperty("angle")
  val angle: Int,
)
