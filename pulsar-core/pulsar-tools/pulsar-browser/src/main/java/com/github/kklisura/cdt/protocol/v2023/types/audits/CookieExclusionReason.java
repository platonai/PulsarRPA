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

public enum CookieExclusionReason {
  @JsonProperty("ExcludeSameSiteUnspecifiedTreatedAsLax")
  EXCLUDE_SAME_SITE_UNSPECIFIED_TREATED_AS_LAX,
  @JsonProperty("ExcludeSameSiteNoneInsecure")
  EXCLUDE_SAME_SITE_NONE_INSECURE,
  @JsonProperty("ExcludeSameSiteLax")
  EXCLUDE_SAME_SITE_LAX,
  @JsonProperty("ExcludeSameSiteStrict")
  EXCLUDE_SAME_SITE_STRICT,
  @JsonProperty("ExcludeInvalidSameParty")
  EXCLUDE_INVALID_SAME_PARTY,
  @JsonProperty("ExcludeSamePartyCrossPartyContext")
  EXCLUDE_SAME_PARTY_CROSS_PARTY_CONTEXT,
  @JsonProperty("ExcludeDomainNonASCII")
  EXCLUDE_DOMAIN_NON_ASCII,
  @JsonProperty("ExcludeThirdPartyCookieBlockedInFirstPartySet")
  EXCLUDE_THIRD_PARTY_COOKIE_BLOCKED_IN_FIRST_PARTY_SET
}
