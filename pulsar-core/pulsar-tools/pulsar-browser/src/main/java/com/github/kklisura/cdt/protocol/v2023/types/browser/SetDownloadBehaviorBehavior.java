package com.github.kklisura.cdt.protocol.v2023.types.browser;

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
 * Whether to allow all or deny all download requests, or use default Chrome behavior if available
 * (otherwise deny). |allowAndName| allows download and names files according to their dowmload
 * guids.
 */
public enum SetDownloadBehaviorBehavior {
  @JsonProperty("deny")
  DENY,
  @JsonProperty("allow")
  ALLOW,
  @JsonProperty("allowAndName")
  ALLOW_AND_NAME,
  @JsonProperty("default")
  DEFAULT
}
