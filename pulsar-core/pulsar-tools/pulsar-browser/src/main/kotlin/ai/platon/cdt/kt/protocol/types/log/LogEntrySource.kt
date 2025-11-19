@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.log

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Log entry source.
 */
public enum class LogEntrySource {
  @JsonProperty("xml")
  XML,
  @JsonProperty("javascript")
  JAVASCRIPT,
  @JsonProperty("network")
  NETWORK,
  @JsonProperty("storage")
  STORAGE,
  @JsonProperty("appcache")
  APPCACHE,
  @JsonProperty("rendering")
  RENDERING,
  @JsonProperty("security")
  SECURITY,
  @JsonProperty("deprecation")
  DEPRECATION,
  @JsonProperty("worker")
  WORKER,
  @JsonProperty("violation")
  VIOLATION,
  @JsonProperty("intervention")
  INTERVENTION,
  @JsonProperty("recommendation")
  RECOMMENDATION,
  @JsonProperty("other")
  OTHER,
}
