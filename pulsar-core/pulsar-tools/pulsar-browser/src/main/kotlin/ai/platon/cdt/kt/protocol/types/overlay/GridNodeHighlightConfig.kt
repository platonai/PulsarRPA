@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.overlay

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int

/**
 * Configurations for Persistent Grid Highlight
 */
data class GridNodeHighlightConfig(
  @param:JsonProperty("gridHighlightConfig")
  val gridHighlightConfig: GridHighlightConfig,
  @param:JsonProperty("nodeId")
  val nodeId: Int,
)
