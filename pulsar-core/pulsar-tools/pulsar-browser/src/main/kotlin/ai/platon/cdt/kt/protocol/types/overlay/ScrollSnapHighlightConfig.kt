@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.overlay

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int

data class ScrollSnapHighlightConfig(
  @param:JsonProperty("scrollSnapContainerHighlightConfig")
  val scrollSnapContainerHighlightConfig: ScrollSnapContainerHighlightConfig,
  @param:JsonProperty("nodeId")
  val nodeId: Int,
)
