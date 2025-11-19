@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.audits

import com.fasterxml.jackson.`annotation`.JsonProperty

public enum class SameSiteCookieExclusionReason {
  @JsonProperty("ExcludeSameSiteUnspecifiedTreatedAsLax")
  EXCLUDE_SAME_SITE_UNSPECIFIED_TREATED_AS_LAX,
  @JsonProperty("ExcludeSameSiteNoneInsecure")
  EXCLUDE_SAME_SITE_NONE_INSECURE,
  @JsonProperty("ExcludeSameSiteLax")
  EXCLUDE_SAME_SITE_LAX,
  @JsonProperty("ExcludeSameSiteStrict")
  EXCLUDE_SAME_SITE_STRICT,
}
