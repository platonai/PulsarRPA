@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.input

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.types.input.DragData
import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Emitted only when `Input.setInterceptDrags` is enabled. Use this data with `Input.dispatchDragEvent` to
 * restore normal drag and drop behavior.
 */
@Experimental
data class DragIntercepted(
  @param:JsonProperty("data")
  val `data`: DragData,
)
