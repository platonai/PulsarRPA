package ai.platon.cdt.kt.protocol.events.browser

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * Fired when page is about to start a download.
 */
@Experimental
public data class DownloadWillBegin(
  @JsonProperty("frameId")
  public val frameId: String,
  @JsonProperty("guid")
  public val guid: String,
  @JsonProperty("url")
  public val url: String,
  @JsonProperty("suggestedFilename")
  public val suggestedFilename: String,
)
