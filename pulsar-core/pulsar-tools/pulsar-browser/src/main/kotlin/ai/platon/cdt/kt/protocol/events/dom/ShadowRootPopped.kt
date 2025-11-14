@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.dom

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int

/**
 * Called when shadow root is popped from the element.
 */
@Experimental
data class ShadowRootPopped(
  @param:JsonProperty("hostId")
  val hostId: Int,
  @param:JsonProperty("rootId")
  val rootId: Int,
)
