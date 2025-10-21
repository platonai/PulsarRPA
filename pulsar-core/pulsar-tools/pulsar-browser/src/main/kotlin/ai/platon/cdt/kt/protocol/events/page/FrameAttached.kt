package ai.platon.cdt.kt.protocol.events.page

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.types.runtime.StackTrace
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * Fired when frame has been attached to its parent.
 */
public data class FrameAttached(
  @JsonProperty("frameId")
  public val frameId: String,
  @JsonProperty("parentFrameId")
  public val parentFrameId: String,
  @JsonProperty("stack")
  @Optional
  public val stack: StackTrace? = null,
)
