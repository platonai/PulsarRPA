@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.overlay

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.types.dom.RGBA
import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Style information for drawing a line.
 */
data class LineStyle(
  @param:JsonProperty("color")
  @param:Optional
  val color: RGBA? = null,
  @param:JsonProperty("pattern")
  @param:Optional
  val pattern: LineStylePattern? = null,
)
