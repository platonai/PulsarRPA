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

/**
 * Enum indicating the reason a response has been blocked. These reasons are refinements of the net
 * error BLOCKED_BY_RESPONSE.
 */
public enum BlockedByResponseReason {
  @JsonProperty("CoepFrameResourceNeedsCoepHeader")
  COEP_FRAME_RESOURCE_NEEDS_COEP_HEADER,
  @JsonProperty("CoopSandboxedIFrameCannotNavigateToCoopPage")
  COOP_SANDBOXED_I_FRAME_CANNOT_NAVIGATE_TO_COOP_PAGE,
  @JsonProperty("CorpNotSameOrigin")
  CORP_NOT_SAME_ORIGIN,
  @JsonProperty("CorpNotSameOriginAfterDefaultedToSameOriginByCoep")
  CORP_NOT_SAME_ORIGIN_AFTER_DEFAULTED_TO_SAME_ORIGIN_BY_COEP,
  @JsonProperty("CorpNotSameSite")
  CORP_NOT_SAME_SITE
}
