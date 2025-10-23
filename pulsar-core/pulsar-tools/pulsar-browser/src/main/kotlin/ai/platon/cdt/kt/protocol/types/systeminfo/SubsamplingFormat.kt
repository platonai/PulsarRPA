@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.systeminfo

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * YUV subsampling type of the pixels of a given image.
 */
public enum class SubsamplingFormat {
  @JsonProperty("yuv420")
  YUV_420,
  @JsonProperty("yuv422")
  YUV_422,
  @JsonProperty("yuv444")
  YUV_444,
}
