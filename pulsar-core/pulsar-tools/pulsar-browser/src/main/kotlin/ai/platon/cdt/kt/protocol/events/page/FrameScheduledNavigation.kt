@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.page

import ai.platon.cdt.kt.protocol.types.page.ClientNavigationReason
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Deprecated
import kotlin.Double
import kotlin.String

/**
 * Fired when frame schedules a potential navigation.
 */
@Deprecated("Deprecated")
data class FrameScheduledNavigation(
  @param:JsonProperty("frameId")
  val frameId: String,
  @param:JsonProperty("delay")
  val delay: Double,
  @param:JsonProperty("reason")
  val reason: ClientNavigationReason,
  @param:JsonProperty("url")
  val url: String,
)
