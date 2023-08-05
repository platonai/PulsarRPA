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
 * Details exposed when memory request explicitly declared. Keep consistent with
 * memory_dump_request_args.h and memory_instrumentation.mojom
 */
public enum MemoryDumpLevelOfDetail {
  @JsonProperty("background")
  BACKGROUND,
  @JsonProperty("light")
  LIGHT,
  @JsonProperty("detailed")
  DETAILED
}
