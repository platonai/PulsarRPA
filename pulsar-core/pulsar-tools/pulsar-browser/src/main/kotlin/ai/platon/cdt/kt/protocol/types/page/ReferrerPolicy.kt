package ai.platon.cdt.kt.protocol.types.page

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * The referring-policy used for the navigation.
 */
public enum class ReferrerPolicy {
  @JsonProperty("noReferrer")
  NO_REFERRER,
  @JsonProperty("noReferrerWhenDowngrade")
  NO_REFERRER_WHEN_DOWNGRADE,
  @JsonProperty("origin")
  ORIGIN,
  @JsonProperty("originWhenCrossOrigin")
  ORIGIN_WHEN_CROSS_ORIGIN,
  @JsonProperty("sameOrigin")
  SAME_ORIGIN,
  @JsonProperty("strictOrigin")
  STRICT_ORIGIN,
  @JsonProperty("strictOriginWhenCrossOrigin")
  STRICT_ORIGIN_WHEN_CROSS_ORIGIN,
  @JsonProperty("unsafeUrl")
  UNSAFE_URL,
}
