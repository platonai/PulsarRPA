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
public data class GPUInfo(
  @JsonProperty("devices")
  public val devices: List<GPUDevice>,
  @JsonProperty("auxAttributes")
  @Optional
  public val auxAttributes: Map<String, Any?>? = null,
  @JsonProperty("featureStatus")
  @Optional
  public val featureStatus: Map<String, Any?>? = null,
  @JsonProperty("driverBugWorkarounds")
  public val driverBugWorkarounds: List<String>,
  @JsonProperty("videoDecoding")
  public val videoDecoding: List<VideoDecodeAcceleratorCapability>,
  @JsonProperty("videoEncoding")
  public val videoEncoding: List<VideoEncodeAcceleratorCapability>,
  @JsonProperty("imageDecoding")
  public val imageDecoding: List<ImageDecodeAcceleratorCapability>,
)
