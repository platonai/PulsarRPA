package ai.platon.cdt.kt.protocol.types.overlay

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.types.dom.RGBA
import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Style information for drawing a box.
 */
public data class BoxStyle(
  @JsonProperty("fillColor")
  @Optional
  public val fillColor: RGBA? = null,
  @JsonProperty("hatchColor")
  @Optional
  public val hatchColor: RGBA? = null,
)
