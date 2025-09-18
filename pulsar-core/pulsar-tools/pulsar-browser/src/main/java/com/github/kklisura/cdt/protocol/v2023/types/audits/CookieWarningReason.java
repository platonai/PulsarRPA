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

public enum CookieWarningReason {
  @JsonProperty("WarnSameSiteUnspecifiedCrossSiteContext")
  WARN_SAME_SITE_UNSPECIFIED_CROSS_SITE_CONTEXT,
  @JsonProperty("WarnSameSiteNoneInsecure")
  WARN_SAME_SITE_NONE_INSECURE,
  @JsonProperty("WarnSameSiteUnspecifiedLaxAllowUnsafe")
  WARN_SAME_SITE_UNSPECIFIED_LAX_ALLOW_UNSAFE,
  @JsonProperty("WarnSameSiteStrictLaxDowngradeStrict")
  WARN_SAME_SITE_STRICT_LAX_DOWNGRADE_STRICT,
  @JsonProperty("WarnSameSiteStrictCrossDowngradeStrict")
  WARN_SAME_SITE_STRICT_CROSS_DOWNGRADE_STRICT,
  @JsonProperty("WarnSameSiteStrictCrossDowngradeLax")
  WARN_SAME_SITE_STRICT_CROSS_DOWNGRADE_LAX,
  @JsonProperty("WarnSameSiteLaxCrossDowngradeStrict")
  WARN_SAME_SITE_LAX_CROSS_DOWNGRADE_STRICT,
  @JsonProperty("WarnSameSiteLaxCrossDowngradeLax")
  WARN_SAME_SITE_LAX_CROSS_DOWNGRADE_LAX,
  @JsonProperty("WarnAttributeValueExceedsMaxSize")
  WARN_ATTRIBUTE_VALUE_EXCEEDS_MAX_SIZE,
  @JsonProperty("WarnDomainNonASCII")
  WARN_DOMAIN_NON_ASCII,
  @JsonProperty("WarnThirdPartyPhaseout")
  WARN_THIRD_PARTY_PHASEOUT
}
