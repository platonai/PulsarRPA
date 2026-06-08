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

public enum PrivateNetworkRequestPolicy {
  @JsonProperty("Allow")
  ALLOW,
  @JsonProperty("BlockFromInsecureToMorePrivate")
  BLOCK_FROM_INSECURE_TO_MORE_PRIVATE,
  @JsonProperty("WarnFromInsecureToMorePrivate")
  WARN_FROM_INSECURE_TO_MORE_PRIVATE,
  @JsonProperty("PreflightBlock")
  PREFLIGHT_BLOCK,
  @JsonProperty("PreflightWarn")
  PREFLIGHT_WARN,
  @JsonProperty("PermissionBlock")
  PERMISSION_BLOCK
}
