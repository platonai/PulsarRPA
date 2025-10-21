package ai.platon.cdt.kt.protocol.types.dom

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Pseudo element type.
 */
public enum class PseudoType {
  @JsonProperty("first-line")
  FIRST_LINE,
  @JsonProperty("first-letter")
  FIRST_LETTER,
  @JsonProperty("before")
  BEFORE,
  @JsonProperty("after")
  AFTER,
  @JsonProperty("marker")
  MARKER,
  @JsonProperty("backdrop")
  BACKDROP,
  @JsonProperty("selection")
  SELECTION,
  @JsonProperty("target-text")
  TARGET_TEXT,
  @JsonProperty("spelling-error")
  SPELLING_ERROR,
  @JsonProperty("grammar-error")
  GRAMMAR_ERROR,
  @JsonProperty("first-line-inherited")
  FIRST_LINE_INHERITED,
  @JsonProperty("scrollbar")
  SCROLLBAR,
  @JsonProperty("scrollbar-thumb")
  SCROLLBAR_THUMB,
  @JsonProperty("scrollbar-button")
  SCROLLBAR_BUTTON,
  @JsonProperty("scrollbar-track")
  SCROLLBAR_TRACK,
  @JsonProperty("scrollbar-track-piece")
  SCROLLBAR_TRACK_PIECE,
  @JsonProperty("scrollbar-corner")
  SCROLLBAR_CORNER,
  @JsonProperty("resizer")
  RESIZER,
  @JsonProperty("input-list-button")
  INPUT_LIST_BUTTON,
}
