package ai.platon.cdt.kt.protocol.types.systeminfo

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int
import kotlin.String

/**
 * Describes a supported video encoding profile with its associated maximum
 * resolution and maximum framerate.
 */
public data class VideoEncodeAcceleratorCapability(
  @JsonProperty("profile")
  public val profile: String,
  @JsonProperty("maxResolution")
  public val maxResolution: Size,
  @JsonProperty("maxFramerateNumerator")
  public val maxFramerateNumerator: Int,
  @JsonProperty("maxFramerateDenominator")
  public val maxFramerateDenominator: Int,
)
