@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.page

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.String

/**
 * Fired when a JavaScript initiated dialog (alert, confirm, prompt, or onbeforeunload) has been
 * closed.
 */
data class JavascriptDialogClosed(
  @param:JsonProperty("result")
  val result: Boolean,
  @param:JsonProperty("userInput")
  val userInput: String,
)
