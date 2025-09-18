package com.github.kklisura.cdt.protocol.v2023.types.dom;

/*-
 * #%L
 * cdt-java-client
 * %%
 * Copyright (C) 2018 - 2023 Kenan Klisura
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.fasterxml.jackson.annotation.JsonProperty;

/** Pseudo element type. */
public enum PseudoType {
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
  @JsonProperty("highlight")
  HIGHLIGHT,
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
  @JsonProperty("view-transition")
  VIEW_TRANSITION,
  @JsonProperty("view-transition-group")
  VIEW_TRANSITION_GROUP,
  @JsonProperty("view-transition-image-pair")
  VIEW_TRANSITION_IMAGE_PAIR,
  @JsonProperty("view-transition-old")
  VIEW_TRANSITION_OLD,
  @JsonProperty("view-transition-new")
  VIEW_TRANSITION_NEW
}
