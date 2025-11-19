@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.network

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * The referrer policy of the request, as defined in https://www.w3.org/TR/referrer-policy/
 */
public enum class RequestReferrerPolicy {
  @JsonProperty("unsafe-url")
  UNSAFE_URL,
  @JsonProperty("no-referrer-when-downgrade")
  NO_REFERRER_WHEN_DOWNGRADE,
  @JsonProperty("no-referrer")
  NO_REFERRER,
  @JsonProperty("origin")
  ORIGIN,
  @JsonProperty("origin-when-cross-origin")
  ORIGIN_WHEN_CROSS_ORIGIN,
  @JsonProperty("same-origin")
  SAME_ORIGIN,
  @JsonProperty("strict-origin")
  STRICT_ORIGIN,
  @JsonProperty("strict-origin-when-cross-origin")
  STRICT_ORIGIN_WHEN_CROSS_ORIGIN,
}
