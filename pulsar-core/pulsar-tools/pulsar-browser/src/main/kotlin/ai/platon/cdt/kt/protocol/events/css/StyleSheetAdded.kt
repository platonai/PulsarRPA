package ai.platon.cdt.kt.protocol.events.css

import ai.platon.cdt.kt.protocol.types.css.CSSStyleSheetHeader
import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Fired whenever an active document stylesheet is added.
 */
public data class StyleSheetAdded(
  @JsonProperty("header")
  public val `header`: CSSStyleSheetHeader,
)
