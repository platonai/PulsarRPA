@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.page

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double

/**
 * Screencast frame metadata.
 */
@Experimental
data class ScreencastFrameMetadata(
  @param:JsonProperty("offsetTop")
  val offsetTop: Double,
  @param:JsonProperty("pageScaleFactor")
  val pageScaleFactor: Double,
  @param:JsonProperty("deviceWidth")
  val deviceWidth: Double,
  @param:JsonProperty("deviceHeight")
  val deviceHeight: Double,
  @param:JsonProperty("scrollOffsetX")
  val scrollOffsetX: Double,
  @param:JsonProperty("scrollOffsetY")
  val scrollOffsetY: Double,
  @param:JsonProperty("timestamp")
  @param:Optional
  val timestamp: Double? = null,
)
