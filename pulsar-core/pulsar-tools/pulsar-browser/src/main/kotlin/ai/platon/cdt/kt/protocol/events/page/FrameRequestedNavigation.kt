@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.page

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.types.page.ClientNavigationDisposition
import ai.platon.cdt.kt.protocol.types.page.ClientNavigationReason
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * Fired when a renderer-initiated navigation is requested.
 * Navigation may still be cancelled after the event is issued.
 */
@Experimental
data class FrameRequestedNavigation(
  @param:JsonProperty("frameId")
  val frameId: String,
  @param:JsonProperty("reason")
  val reason: ClientNavigationReason,
  @param:JsonProperty("url")
  val url: String,
  @param:JsonProperty("disposition")
  val disposition: ClientNavigationDisposition,
)
