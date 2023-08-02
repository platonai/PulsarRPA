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

/** Types of reasons why a cookie may not be sent with a request. */
public enum CookieBlockedReason {
  @JsonProperty("SecureOnly")
  SECURE_ONLY,
  @JsonProperty("NotOnPath")
  NOT_ON_PATH,
  @JsonProperty("DomainMismatch")
  DOMAIN_MISMATCH,
  @JsonProperty("SameSiteStrict")
  SAME_SITE_STRICT,
  @JsonProperty("SameSiteLax")
  SAME_SITE_LAX,
  @JsonProperty("SameSiteUnspecifiedTreatedAsLax")
  SAME_SITE_UNSPECIFIED_TREATED_AS_LAX,
  @JsonProperty("SameSiteNoneInsecure")
  SAME_SITE_NONE_INSECURE,
  @JsonProperty("UserPreferences")
  USER_PREFERENCES,
  @JsonProperty("ThirdPartyBlockedInFirstPartySet")
  THIRD_PARTY_BLOCKED_IN_FIRST_PARTY_SET,
  @JsonProperty("UnknownError")
  UNKNOWN_ERROR,
  @JsonProperty("SchemefulSameSiteStrict")
  SCHEMEFUL_SAME_SITE_STRICT,
  @JsonProperty("SchemefulSameSiteLax")
  SCHEMEFUL_SAME_SITE_LAX,
  @JsonProperty("SchemefulSameSiteUnspecifiedTreatedAsLax")
  SCHEMEFUL_SAME_SITE_UNSPECIFIED_TREATED_AS_LAX,
  @JsonProperty("SamePartyFromCrossPartyContext")
  SAME_PARTY_FROM_CROSS_PARTY_CONTEXT,
  @JsonProperty("NameValuePairExceedsMaxSize")
  NAME_VALUE_PAIR_EXCEEDS_MAX_SIZE
}
