@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.page

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.types.page.ScreencastFrameMetadata
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int
import kotlin.String

/**
 * Compressed image data requested by the `startScreencast`.
 */
@Experimental
data class ScreencastFrame(
  @param:JsonProperty("data")
  val `data`: String,
  @param:JsonProperty("metadata")
  val metadata: ScreencastFrameMetadata,
  @param:JsonProperty("sessionId")
  val sessionId: Int,
)
