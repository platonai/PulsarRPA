@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.overlay

import ai.platon.cdt.kt.protocol.types.dom.RGBA
import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Configuration data for drawing the source order of an elements children.
 */
data class SourceOrderConfig(
  @param:JsonProperty("parentOutlineColor")
  val parentOutlineColor: RGBA,
  @param:JsonProperty("childOutlineColor")
  val childOutlineColor: RGBA,
)
