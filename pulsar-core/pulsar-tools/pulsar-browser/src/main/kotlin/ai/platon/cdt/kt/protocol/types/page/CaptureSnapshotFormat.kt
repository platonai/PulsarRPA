package ai.platon.cdt.kt.protocol.types.page

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Format (defaults to mhtml).
 */
public enum class CaptureSnapshotFormat {
  @JsonProperty("mhtml")
  MHTML,
}
