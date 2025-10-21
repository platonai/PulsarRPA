package ai.platon.cdt.kt.protocol.types.page

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.collections.List

/**
 * Information about the Frame hierarchy.
 */
public data class FrameTree(
  @JsonProperty("frame")
  public val frame: Frame,
  @JsonProperty("childFrames")
  @Optional
  public val childFrames: List<FrameTree>? = null,
)
