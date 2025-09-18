package com.github.kklisura.cdt.protocol.v2023.types.network;

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

/** The underlying connection technology that the browser is supposedly using. */
public enum ConnectionType {
  @JsonProperty("none")
  NONE,
  @JsonProperty("cellular2g")
  CELLULAR_2G,
  @JsonProperty("cellular3g")
  CELLULAR_3G,
  @JsonProperty("cellular4g")
  CELLULAR_4G,
  @JsonProperty("bluetooth")
  BLUETOOTH,
  @JsonProperty("ethernet")
  ETHERNET,
  @JsonProperty("wifi")
  WIFI,
  @JsonProperty("wimax")
  WIMAX,
  @JsonProperty("other")
  OTHER
}
