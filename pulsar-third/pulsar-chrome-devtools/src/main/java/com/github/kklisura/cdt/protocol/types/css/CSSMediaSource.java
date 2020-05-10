package com.github.kklisura.cdt.protocol.types.css;

/*-
 * #%L
 * cdt-java-client
 * %%
 * Copyright (C) 2018 - 2019 Kenan Klisura
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
 * Source of the media query: "mediaRule" if specified by a @media rule, "importRule" if specified
 * by an @import rule, "linkedSheet" if specified by a "media" attribute in a linked stylesheet's
 * LINK tag, "inlineSheet" if specified by a "media" attribute in an inline stylesheet's STYLE tag.
 */
public enum CSSMediaSource {
  @JsonProperty("mediaRule")
  MEDIA_RULE,
  @JsonProperty("importRule")
  IMPORT_RULE,
  @JsonProperty("linkedSheet")
  LINKED_SHEET,
  @JsonProperty("inlineSheet")
  INLINE_SHEET
}
