package com.github.kklisura.cdt.protocol.v2023.types.accessibility;

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

/**
 * Values of AXProperty name: - from 'busy' to 'roledescription': states which apply to every AX
 * node - from 'live' to 'root': attributes which apply to nodes in live regions - from
 * 'autocomplete' to 'valuetext': attributes which apply to widgets - from 'checked' to 'selected':
 * states which apply to widgets - from 'activedescendant' to 'owns' - relationships between
 * elements other than parent/child/sibling.
 */
public enum AXPropertyName {
  @JsonProperty("busy")
  BUSY,
  @JsonProperty("disabled")
  DISABLED,
  @JsonProperty("editable")
  EDITABLE,
  @JsonProperty("focusable")
  FOCUSABLE,
  @JsonProperty("focused")
  FOCUSED,
  @JsonProperty("hidden")
  HIDDEN,
  @JsonProperty("hiddenRoot")
  HIDDEN_ROOT,
  @JsonProperty("invalid")
  INVALID,
  @JsonProperty("keyshortcuts")
  KEYSHORTCUTS,
  @JsonProperty("settable")
  SETTABLE,
  @JsonProperty("roledescription")
  ROLEDESCRIPTION,
  @JsonProperty("live")
  LIVE,
  @JsonProperty("atomic")
  ATOMIC,
  @JsonProperty("relevant")
  RELEVANT,
  @JsonProperty("root")
  ROOT,
  @JsonProperty("autocomplete")
  AUTOCOMPLETE,
  @JsonProperty("hasPopup")
  HAS_POPUP,
  @JsonProperty("level")
  LEVEL,
  @JsonProperty("multiselectable")
  MULTISELECTABLE,
  @JsonProperty("orientation")
  ORIENTATION,
  @JsonProperty("multiline")
  MULTILINE,
  @JsonProperty("readonly")
  READONLY,
  @JsonProperty("required")
  REQUIRED,
  @JsonProperty("valuemin")
  VALUEMIN,
  @JsonProperty("valuemax")
  VALUEMAX,
  @JsonProperty("valuetext")
  VALUETEXT,
  @JsonProperty("checked")
  CHECKED,
  @JsonProperty("expanded")
  EXPANDED,
  @JsonProperty("modal")
  MODAL,
  @JsonProperty("pressed")
  PRESSED,
  @JsonProperty("selected")
  SELECTED,
  @JsonProperty("activedescendant")
  ACTIVEDESCENDANT,
  @JsonProperty("controls")
  CONTROLS,
  @JsonProperty("describedby")
  DESCRIBEDBY,
  @JsonProperty("details")
  DETAILS,
  @JsonProperty("errormessage")
  ERRORMESSAGE,
  @JsonProperty("flowto")
  FLOWTO,
  @JsonProperty("labelledby")
  LABELLEDBY,
  @JsonProperty("owns")
  OWNS
}
