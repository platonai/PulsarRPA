@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.page

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int

/**
 * Layout viewport position and dimensions.
 */
data class LayoutViewport(
  @param:JsonProperty("pageX")
  val pageX: Int,
  @param:JsonProperty("pageY")
  val pageY: Int,
  @param:JsonProperty("clientWidth")
  val clientWidth: Int,
  @param:JsonProperty("clientHeight")
  val clientHeight: Int,
)
