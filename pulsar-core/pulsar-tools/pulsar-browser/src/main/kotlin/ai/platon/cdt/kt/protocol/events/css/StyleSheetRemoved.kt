package ai.platon.cdt.kt.protocol.events.css

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * Fired whenever an active document stylesheet is removed.
 */
public data class StyleSheetRemoved(
  @JsonProperty("styleSheetId")
  public val styleSheetId: String,
)
