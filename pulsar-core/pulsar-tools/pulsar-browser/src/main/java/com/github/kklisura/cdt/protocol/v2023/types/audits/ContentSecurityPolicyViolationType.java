package com.github.kklisura.cdt.protocol.v2023.types.audits;

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

public enum ContentSecurityPolicyViolationType {
  @JsonProperty("kInlineViolation")
  K_INLINE_VIOLATION,
  @JsonProperty("kEvalViolation")
  K_EVAL_VIOLATION,
  @JsonProperty("kURLViolation")
  K_URL_VIOLATION,
  @JsonProperty("kTrustedTypesSinkViolation")
  K_TRUSTED_TYPES_SINK_VIOLATION,
  @JsonProperty("kTrustedTypesPolicyViolation")
  K_TRUSTED_TYPES_POLICY_VIOLATION,
  @JsonProperty("kWasmEvalViolation")
  K_WASM_EVAL_VIOLATION
}
