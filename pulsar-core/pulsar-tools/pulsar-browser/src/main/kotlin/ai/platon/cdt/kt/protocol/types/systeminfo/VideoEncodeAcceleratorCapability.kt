@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.systeminfo

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int
import kotlin.String

/**
 * Describes a supported video encoding profile with its associated maximum
 * resolution and maximum framerate.
 */
data class VideoEncodeAcceleratorCapability(
  @param:JsonProperty("profile")
  val profile: String,
  @param:JsonProperty("maxResolution")
  val maxResolution: Size,
  @param:JsonProperty("maxFramerateNumerator")
  val maxFramerateNumerator: Int,
  @param:JsonProperty("maxFramerateDenominator")
  val maxFramerateDenominator: Int,
)
