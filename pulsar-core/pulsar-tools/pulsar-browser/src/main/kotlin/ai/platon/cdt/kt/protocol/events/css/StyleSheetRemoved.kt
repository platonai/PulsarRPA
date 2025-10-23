@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.css

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * Fired whenever an active document stylesheet is removed.
 */
data class StyleSheetRemoved(
  @param:JsonProperty("styleSheetId")
  val styleSheetId: String,
)
