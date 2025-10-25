@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.overlay

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int

data class FlexNodeHighlightConfig(
  @param:JsonProperty("flexContainerHighlightConfig")
  val flexContainerHighlightConfig: FlexContainerHighlightConfig,
  @param:JsonProperty("nodeId")
  val nodeId: Int,
)
