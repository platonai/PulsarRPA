package ai.platon.cdt.kt.protocol.types.overlay

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.types.dom.RGBA
import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Style information for drawing a line.
 */
public data class LineStyle(
  @JsonProperty("color")
  @Optional
  public val color: RGBA? = null,
  @JsonProperty("pattern")
  @Optional
  public val pattern: LineStylePattern? = null,
)
