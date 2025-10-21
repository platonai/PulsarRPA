package ai.platon.cdt.kt.protocol.types.dom

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double

/**
 * Rectangle.
 */
public data class Rect(
  @JsonProperty("x")
  public val x: Double,
  @JsonProperty("y")
  public val y: Double,
  @JsonProperty("width")
  public val width: Double,
  @JsonProperty("height")
  public val height: Double,
)
