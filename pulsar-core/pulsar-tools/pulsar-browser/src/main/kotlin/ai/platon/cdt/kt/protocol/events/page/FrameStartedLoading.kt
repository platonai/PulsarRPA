package ai.platon.cdt.kt.protocol.events.page

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * Fired when frame has started loading.
 */
@Experimental
public data class FrameStartedLoading(
  @JsonProperty("frameId")
  public val frameId: String,
)
