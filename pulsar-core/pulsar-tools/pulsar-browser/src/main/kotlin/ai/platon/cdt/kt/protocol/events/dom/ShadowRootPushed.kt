@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.dom

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.types.dom.Node
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int

/**
 * Called when shadow root is pushed into the element.
 */
@Experimental
data class ShadowRootPushed(
  @param:JsonProperty("hostId")
  val hostId: Int,
  @param:JsonProperty("root")
  val root: Node,
)
