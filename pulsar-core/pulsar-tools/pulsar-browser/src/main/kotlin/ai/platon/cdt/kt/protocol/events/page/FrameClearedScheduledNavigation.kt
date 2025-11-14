@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.page

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Deprecated
import kotlin.String

/**
 * Fired when frame no longer has a scheduled navigation.
 */
@Deprecated("Deprecated")
data class FrameClearedScheduledNavigation(
  @param:JsonProperty("frameId")
  val frameId: String,
)
