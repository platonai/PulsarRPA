@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.page

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.types.page.FileChooserOpenedMode
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int
import kotlin.String

/**
 * Emitted only when `page.interceptFileChooser` is enabled.
 */
data class FileChooserOpened(
  @param:JsonProperty("frameId")
  @param:Experimental
  val frameId: String,
  @param:JsonProperty("backendNodeId")
  @param:Experimental
  val backendNodeId: Int,
  @param:JsonProperty("mode")
  val mode: FileChooserOpenedMode,
)
