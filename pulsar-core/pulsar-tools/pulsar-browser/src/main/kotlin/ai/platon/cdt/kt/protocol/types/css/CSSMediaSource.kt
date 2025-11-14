@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.css

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Source of the media query: "mediaRule" if specified by a @media rule, "importRule" if
 * specified by an @import rule, "linkedSheet" if specified by a "media" attribute in a linked
 * stylesheet's LINK tag, "inlineSheet" if specified by a "media" attribute in an inline
 * stylesheet's STYLE tag.
 */
public enum class CSSMediaSource {
  @JsonProperty("mediaRule")
  MEDIA_RULE,
  @JsonProperty("importRule")
  IMPORT_RULE,
  @JsonProperty("linkedSheet")
  LINKED_SHEET,
  @JsonProperty("inlineSheet")
  INLINE_SHEET,
}
