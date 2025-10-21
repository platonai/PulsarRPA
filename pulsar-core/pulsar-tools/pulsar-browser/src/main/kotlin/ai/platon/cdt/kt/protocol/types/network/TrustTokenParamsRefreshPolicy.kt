package ai.platon.cdt.kt.protocol.types.network

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Only set for "token-redemption" type and determine whether
 * to request a fresh SRR or use a still valid cached SRR.
 */
public enum class TrustTokenParamsRefreshPolicy {
  @JsonProperty("UseCached")
  USE_CACHED,
  @JsonProperty("Refresh")
  REFRESH,
}
