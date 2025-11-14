@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.network

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Types of reasons why a cookie may not be sent with a request.
 */
public enum class CookieBlockedReason {
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
}
