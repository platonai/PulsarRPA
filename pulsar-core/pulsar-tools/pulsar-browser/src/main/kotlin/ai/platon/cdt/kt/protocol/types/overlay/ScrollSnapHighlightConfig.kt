package ai.platon.cdt.kt.protocol.types.overlay

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int

public data class ScrollSnapHighlightConfig(
  @JsonProperty("scrollSnapContainerHighlightConfig")
  public val scrollSnapContainerHighlightConfig: ScrollSnapContainerHighlightConfig,
  @JsonProperty("nodeId")
  public val nodeId: Int,
)
