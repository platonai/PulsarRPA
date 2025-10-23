@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.page

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.types.page.DownloadProgressState
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Deprecated
import kotlin.Double
import kotlin.String

/**
 * Fired when download makes progress. Last call has |done| == true.
 * Deprecated. Use Browser.downloadProgress instead.
 */
@Experimental
@Deprecated("Deprecated")
data class DownloadProgress(
  @param:JsonProperty("guid")
  val guid: String,
  @param:JsonProperty("totalBytes")
  val totalBytes: Double,
  @param:JsonProperty("receivedBytes")
  val receivedBytes: Double,
  @param:JsonProperty("state")
  val state: DownloadProgressState,
)
