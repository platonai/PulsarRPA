package com.github.kklisura.cdt.protocol.v2023.types.tracing;

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
 * Backend type to use for tracing. `chrome` uses the Chrome-integrated tracing service and is
 * supported on all platforms. `system` is only supported on Chrome OS and uses the Perfetto system
 * tracing service. `auto` chooses `system` when the perfettoConfig provided to Tracing.start
 * specifies at least one non-Chrome data source; otherwise uses `chrome`.
 */
public enum TracingBackend {
  @JsonProperty("auto")
  AUTO,
  @JsonProperty("chrome")
  CHROME,
  @JsonProperty("system")
  SYSTEM
}
