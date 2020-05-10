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

/** The referrer policy of the request, as defined in https://www.w3.org/TR/referrer-policy/ */
public enum RequestReferrerPolicy {
  @JsonProperty("unsafe-url")
  UNSAFE_URL,
  @JsonProperty("no-referrer-when-downgrade")
  NO_REFERRER_WHEN_DOWNGRADE,
  @JsonProperty("no-referrer")
  NO_REFERRER,
  @JsonProperty("origin")
  ORIGIN,
  @JsonProperty("origin-when-cross-origin")
  ORIGIN_WHEN_CROSS_ORIGIN,
  @JsonProperty("same-origin")
  SAME_ORIGIN,
  @JsonProperty("strict-origin")
  STRICT_ORIGIN,
  @JsonProperty("strict-origin-when-cross-origin")
  STRICT_ORIGIN_WHEN_CROSS_ORIGIN
}
