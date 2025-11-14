@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.page

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * Fired when frame has stopped loading.
 */
@Experimental
data class FrameStoppedLoading(
  @param:JsonProperty("frameId")
  val frameId: String,
)
