package ai.platon.cdt.kt.protocol.types.performancetimeline

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.Int
import kotlin.String

/**
 * See https://github.com/WICG/LargestContentfulPaint and largest_contentful_paint.idl
 */
public data class LargestContentfulPaint(
  @JsonProperty("renderTime")
  public val renderTime: Double,
  @JsonProperty("loadTime")
  public val loadTime: Double,
  @JsonProperty("size")
  public val size: Double,
  @JsonProperty("elementId")
  @Optional
  public val elementId: String? = null,
  @JsonProperty("url")
  @Optional
  public val url: String? = null,
  @JsonProperty("nodeId")
  @Optional
  public val nodeId: Int? = null,
)
