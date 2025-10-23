@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.page

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.types.page.FrameDetachedReason
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * Fired when frame has been detached from its parent.
 */
data class FrameDetached(
  @param:JsonProperty("frameId")
  val frameId: String,
  @param:JsonProperty("reason")
  @param:Experimental
  val reason: FrameDetachedReason,
)
