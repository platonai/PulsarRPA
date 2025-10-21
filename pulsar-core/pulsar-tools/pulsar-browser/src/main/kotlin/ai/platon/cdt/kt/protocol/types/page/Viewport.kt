package ai.platon.cdt.kt.protocol.types.page

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double

/**
 * Viewport for capturing screenshot.
 */
public data class Viewport(
  @get:JsonProperty("x")
  public val x: Double,
  @get:JsonProperty("y")
  public val y: Double,
  @get:JsonProperty("width")
  public val width: Double,
  @get:JsonProperty("height")
  public val height: Double,
  @get:JsonProperty("scale")
  public val scale: Double,
)
