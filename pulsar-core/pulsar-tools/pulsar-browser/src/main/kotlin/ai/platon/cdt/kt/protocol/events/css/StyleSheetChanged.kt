@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.css

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * Fired whenever a stylesheet is changed as a result of the client operation.
 */
data class StyleSheetChanged(
  @param:JsonProperty("styleSheetId")
  val styleSheetId: String,
)
