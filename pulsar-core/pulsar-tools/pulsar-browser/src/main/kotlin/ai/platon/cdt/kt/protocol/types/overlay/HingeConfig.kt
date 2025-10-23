@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.overlay

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.types.dom.RGBA
import ai.platon.cdt.kt.protocol.types.dom.Rect
import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Configuration for dual screen hinge
 */
data class HingeConfig(
  @param:JsonProperty("rect")
  val rect: Rect,
  @param:JsonProperty("contentColor")
  @param:Optional
  val contentColor: RGBA? = null,
  @param:JsonProperty("outlineColor")
  @param:Optional
  val outlineColor: RGBA? = null,
)
