@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.browser

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Download status.
 */
public enum class DownloadProgressState {
  @JsonProperty("inProgress")
  IN_PROGRESS,
  @JsonProperty("completed")
  COMPLETED,
  @JsonProperty("canceled")
  CANCELED,
}
