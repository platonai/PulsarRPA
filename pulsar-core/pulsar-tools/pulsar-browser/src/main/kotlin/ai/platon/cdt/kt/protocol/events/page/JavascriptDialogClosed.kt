package ai.platon.cdt.kt.protocol.events.page

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.String

/**
 * Fired when a JavaScript initiated dialog (alert, confirm, prompt, or onbeforeunload) has been
 * closed.
 */
public data class JavascriptDialogClosed(
  @JsonProperty("result")
  public val result: Boolean,
  @JsonProperty("userInput")
  public val userInput: String,
)
