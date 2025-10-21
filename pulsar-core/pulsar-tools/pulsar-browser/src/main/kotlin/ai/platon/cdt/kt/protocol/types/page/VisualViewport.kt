package ai.platon.cdt.kt.protocol.types.page

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double

/**
 * Visual viewport position, dimensions, and scale.
 */
public data class VisualViewport(
  @JsonProperty("offsetX")
  public val offsetX: Double,
  @JsonProperty("offsetY")
  public val offsetY: Double,
  @JsonProperty("pageX")
  public val pageX: Double,
  @JsonProperty("pageY")
  public val pageY: Double,
  @JsonProperty("clientWidth")
  public val clientWidth: Double,
  @JsonProperty("clientHeight")
  public val clientHeight: Double,
  @JsonProperty("scale")
  public val scale: Double,
  @JsonProperty("zoom")
  @Optional
  public val zoom: Double? = null,
)
