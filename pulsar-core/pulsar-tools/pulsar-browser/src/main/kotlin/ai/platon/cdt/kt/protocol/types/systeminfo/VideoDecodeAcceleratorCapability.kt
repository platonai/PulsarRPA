package ai.platon.cdt.kt.protocol.types.systeminfo

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * Describes a supported video decoding profile with its associated minimum and
 * maximum resolutions.
 */
public data class VideoDecodeAcceleratorCapability(
  @JsonProperty("profile")
  public val profile: String,
  @JsonProperty("maxResolution")
  public val maxResolution: Size,
  @JsonProperty("minResolution")
  public val minResolution: Size,
)
