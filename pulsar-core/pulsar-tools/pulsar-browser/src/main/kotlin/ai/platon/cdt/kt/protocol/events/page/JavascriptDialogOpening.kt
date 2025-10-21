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
public data class JavascriptDialogOpening(
  @JsonProperty("url")
  public val url: String,
  @JsonProperty("message")
  public val message: String,
  @JsonProperty("type")
  public val type: DialogType,
  @JsonProperty("hasBrowserHandler")
  public val hasBrowserHandler: Boolean,
  @JsonProperty("defaultPrompt")
  @Optional
  public val defaultPrompt: String? = null,
)
