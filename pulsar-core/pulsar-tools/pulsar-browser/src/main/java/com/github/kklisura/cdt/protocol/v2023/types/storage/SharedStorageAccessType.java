package com.github.kklisura.cdt.protocol.v2023.types.storage;

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

/** Enum of shared storage access types. */
public enum SharedStorageAccessType {
  @JsonProperty("documentAddModule")
  DOCUMENT_ADD_MODULE,
  @JsonProperty("documentSelectURL")
  DOCUMENT_SELECT_URL,
  @JsonProperty("documentRun")
  DOCUMENT_RUN,
  @JsonProperty("documentSet")
  DOCUMENT_SET,
  @JsonProperty("documentAppend")
  DOCUMENT_APPEND,
  @JsonProperty("documentDelete")
  DOCUMENT_DELETE,
  @JsonProperty("documentClear")
  DOCUMENT_CLEAR,
  @JsonProperty("workletSet")
  WORKLET_SET,
  @JsonProperty("workletAppend")
  WORKLET_APPEND,
  @JsonProperty("workletDelete")
  WORKLET_DELETE,
  @JsonProperty("workletClear")
  WORKLET_CLEAR,
  @JsonProperty("workletGet")
  WORKLET_GET,
  @JsonProperty("workletKeys")
  WORKLET_KEYS,
  @JsonProperty("workletEntries")
  WORKLET_ENTRIES,
  @JsonProperty("workletLength")
  WORKLET_LENGTH,
  @JsonProperty("workletRemainingBudget")
  WORKLET_REMAINING_BUDGET
}
