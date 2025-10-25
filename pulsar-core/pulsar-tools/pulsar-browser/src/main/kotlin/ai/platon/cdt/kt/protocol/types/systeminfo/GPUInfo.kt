@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.systeminfo

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Any
import kotlin.String
import kotlin.collections.List
import kotlin.collections.Map

/**
 * Provides information about the GPU(s) on the system.
 */
data class GPUInfo(
  @param:JsonProperty("devices")
  val devices: List<GPUDevice>,
  @param:JsonProperty("auxAttributes")
  @param:Optional
  val auxAttributes: Map<String, Any?>? = null,
  @param:JsonProperty("featureStatus")
  @param:Optional
  val featureStatus: Map<String, Any?>? = null,
  @param:JsonProperty("driverBugWorkarounds")
  val driverBugWorkarounds: List<String>,
  @param:JsonProperty("videoDecoding")
  val videoDecoding: List<VideoDecodeAcceleratorCapability>,
  @param:JsonProperty("videoEncoding")
  val videoEncoding: List<VideoEncodeAcceleratorCapability>,
  @param:JsonProperty("imageDecoding")
  val imageDecoding: List<ImageDecodeAcceleratorCapability>,
)
