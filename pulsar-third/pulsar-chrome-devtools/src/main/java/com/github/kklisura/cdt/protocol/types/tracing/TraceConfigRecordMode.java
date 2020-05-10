package com.github.kklisura.cdt.protocol.types.tracing;

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

/** Controls how the trace buffer stores data. */
public enum TraceConfigRecordMode {
  @JsonProperty("recordUntilFull")
  RECORD_UNTIL_FULL,
  @JsonProperty("recordContinuously")
  RECORD_CONTINUOUSLY,
  @JsonProperty("recordAsMuchAsPossible")
  RECORD_AS_MUCH_AS_POSSIBLE,
  @JsonProperty("echoToConsole")
  ECHO_TO_CONSOLE
}
