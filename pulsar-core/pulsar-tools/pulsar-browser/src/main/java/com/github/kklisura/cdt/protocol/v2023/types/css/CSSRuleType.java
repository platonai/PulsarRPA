package com.github.kklisura.cdt.protocol.v2023.types.css;

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
 * Enum indicating the type of a CSS rule, used to represent the order of a style rule's ancestors.
 * This list only contains rule types that are collected during the ancestor rule collection.
 */
public enum CSSRuleType {
  @JsonProperty("MediaRule")
  MEDIA_RULE,
  @JsonProperty("SupportsRule")
  SUPPORTS_RULE,
  @JsonProperty("ContainerRule")
  CONTAINER_RULE,
  @JsonProperty("LayerRule")
  LAYER_RULE,
  @JsonProperty("ScopeRule")
  SCOPE_RULE,
  @JsonProperty("StyleRule")
  STYLE_RULE
}
