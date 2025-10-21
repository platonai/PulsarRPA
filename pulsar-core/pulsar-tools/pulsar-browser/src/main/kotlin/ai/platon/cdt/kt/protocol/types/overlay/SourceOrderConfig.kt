package ai.platon.cdt.kt.protocol.types.overlay

import ai.platon.cdt.kt.protocol.types.dom.RGBA
import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Configuration data for drawing the source order of an elements children.
 */
public data class SourceOrderConfig(
  @JsonProperty("parentOutlineColor")
  public val parentOutlineColor: RGBA,
  @JsonProperty("childOutlineColor")
  public val childOutlineColor: RGBA,
)
