package ai.platon.cdt.kt.protocol.types.systeminfo

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.collections.List

/**
 * Describes a supported image decoding profile with its associated minimum and
 * maximum resolutions and subsampling.
 */
public data class ImageDecodeAcceleratorCapability(
  @JsonProperty("imageType")
  public val imageType: ImageType,
  @JsonProperty("maxDimensions")
  public val maxDimensions: Size,
  @JsonProperty("minDimensions")
  public val minDimensions: Size,
  @JsonProperty("subsamplings")
  public val subsamplings: List<SubsamplingFormat>,
)
