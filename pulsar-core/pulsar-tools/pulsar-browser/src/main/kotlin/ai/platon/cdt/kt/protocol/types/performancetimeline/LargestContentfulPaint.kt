@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.performancetimeline

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.Int
import kotlin.String

/**
 * See https://github.com/WICG/LargestContentfulPaint and largest_contentful_paint.idl
 */
data class LargestContentfulPaint(
  @param:JsonProperty("renderTime")
  val renderTime: Double,
  @param:JsonProperty("loadTime")
  val loadTime: Double,
  @param:JsonProperty("size")
  val size: Double,
  @param:JsonProperty("elementId")
  @param:Optional
  val elementId: String? = null,
  @param:JsonProperty("url")
  @param:Optional
  val url: String? = null,
  @param:JsonProperty("nodeId")
  @param:Optional
  val nodeId: Int? = null,
)
