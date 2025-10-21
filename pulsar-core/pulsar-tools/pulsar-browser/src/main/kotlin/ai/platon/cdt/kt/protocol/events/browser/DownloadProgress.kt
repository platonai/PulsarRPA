package ai.platon.cdt.kt.protocol.events.browser

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.types.browser.DownloadProgressState
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.String

/**
 * Fired when download makes progress. Last call has |done| == true.
 */
@Experimental
public data class DownloadProgress(
  @JsonProperty("guid")
  public val guid: String,
  @JsonProperty("totalBytes")
  public val totalBytes: Double,
  @JsonProperty("receivedBytes")
  public val receivedBytes: Double,
  @JsonProperty("state")
  public val state: DownloadProgressState,
)
