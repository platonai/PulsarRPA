@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.systeminfo

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.collections.List

/**
 * Describes a supported image decoding profile with its associated minimum and
 * maximum resolutions and subsampling.
 */
data class ImageDecodeAcceleratorCapability(
  @param:JsonProperty("imageType")
  val imageType: ImageType,
  @param:JsonProperty("maxDimensions")
  val maxDimensions: Size,
  @param:JsonProperty("minDimensions")
  val minDimensions: Size,
  @param:JsonProperty("subsamplings")
  val subsamplings: List<SubsamplingFormat>,
)
