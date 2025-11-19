@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.browser

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * Fired when page is about to start a download.
 */
@Experimental
data class DownloadWillBegin(
  @param:JsonProperty("frameId")
  val frameId: String,
  @param:JsonProperty("guid")
  val guid: String,
  @param:JsonProperty("url")
  val url: String,
  @param:JsonProperty("suggestedFilename")
  val suggestedFilename: String,
)
