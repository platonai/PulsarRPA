@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.page

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.types.runtime.StackTrace
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * Fired when frame has been attached to its parent.
 */
data class FrameAttached(
  @param:JsonProperty("frameId")
  val frameId: String,
  @param:JsonProperty("parentFrameId")
  val parentFrameId: String,
  @param:JsonProperty("stack")
  @param:Optional
  val stack: StackTrace? = null,
)
