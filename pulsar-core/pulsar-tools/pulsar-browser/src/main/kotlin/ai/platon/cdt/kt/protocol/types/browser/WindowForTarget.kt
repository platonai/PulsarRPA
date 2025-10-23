@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.browser

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int

data class WindowForTarget(
  @param:JsonProperty("windowId")
  val windowId: Int,
  @param:JsonProperty("bounds")
  val bounds: Bounds,
)
