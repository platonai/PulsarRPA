package ai.platon.cdt.kt.protocol.types.overlay

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int

public data class FlexNodeHighlightConfig(
  @JsonProperty("flexContainerHighlightConfig")
  public val flexContainerHighlightConfig: FlexContainerHighlightConfig,
  @JsonProperty("nodeId")
  public val nodeId: Int,
)
