package ai.platon.cdt.kt.protocol.events.page

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.types.page.Frame
import ai.platon.cdt.kt.protocol.types.page.NavigationType
import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Fired once navigation of the frame has completed. Frame is now associated with the new loader.
 */
public data class FrameNavigated(
  @JsonProperty("frame")
  public val frame: Frame,
  @JsonProperty("type")
  @Experimental
  public val type: NavigationType,
)
