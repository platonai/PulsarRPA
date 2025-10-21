package ai.platon.cdt.kt.protocol.events.page

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.types.page.FileChooserOpenedMode
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int
import kotlin.String

/**
 * Emitted only when `page.interceptFileChooser` is enabled.
 */
public data class FileChooserOpened(
  @JsonProperty("frameId")
  @Experimental
  public val frameId: String,
  @JsonProperty("backendNodeId")
  @Experimental
  public val backendNodeId: Int,
  @JsonProperty("mode")
  public val mode: FileChooserOpenedMode,
)
