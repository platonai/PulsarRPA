package com.github.kklisura.cdt.protocol.types.network;

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

/** The reason why request was blocked. */
public enum BlockedReason {
  @JsonProperty("other")
  OTHER,
  @JsonProperty("csp")
  CSP,
  @JsonProperty("mixed-content")
  MIXED_CONTENT,
  @JsonProperty("origin")
  ORIGIN,
  @JsonProperty("inspector")
  INSPECTOR,
  @JsonProperty("subresource-filter")
  SUBRESOURCE_FILTER,
  @JsonProperty("content-type")
  CONTENT_TYPE,
  @JsonProperty("collapsed-by-client")
  COLLAPSED_BY_CLIENT
}
