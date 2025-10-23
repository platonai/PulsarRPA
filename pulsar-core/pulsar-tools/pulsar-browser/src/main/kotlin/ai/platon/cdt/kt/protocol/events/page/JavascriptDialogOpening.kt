@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.page

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.types.page.DialogType
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.String

/**
 * Fired when a JavaScript initiated dialog (alert, confirm, prompt, or onbeforeunload) is about to
 * open.
 */
data class JavascriptDialogOpening(
  @param:JsonProperty("url")
  val url: String,
  @param:JsonProperty("message")
  val message: String,
  @param:JsonProperty("type")
  val type: DialogType,
  @param:JsonProperty("hasBrowserHandler")
  val hasBrowserHandler: Boolean,
  @param:JsonProperty("defaultPrompt")
  @param:Optional
  val defaultPrompt: String? = null,
)
