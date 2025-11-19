@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.systeminfo

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * Describes a supported video decoding profile with its associated minimum and
 * maximum resolutions.
 */
data class VideoDecodeAcceleratorCapability(
  @param:JsonProperty("profile")
  val profile: String,
  @param:JsonProperty("maxResolution")
  val maxResolution: Size,
  @param:JsonProperty("minResolution")
  val minResolution: Size,
)
