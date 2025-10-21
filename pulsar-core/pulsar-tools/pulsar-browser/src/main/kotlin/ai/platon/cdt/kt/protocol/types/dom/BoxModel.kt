package ai.platon.cdt.kt.protocol.types.dom

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.Int
import kotlin.collections.List

/**
 * Box model.
 */
public data class BoxModel(
  @JsonProperty("content")
  public val content: List<Double>,
  @JsonProperty("padding")
  public val padding: List<Double>,
  @JsonProperty("border")
  public val border: List<Double>,
  @JsonProperty("margin")
  public val margin: List<Double>,
  @JsonProperty("width")
  public val width: Int,
  @JsonProperty("height")
  public val height: Int,
  @JsonProperty("shapeOutside")
  @Optional
  public val shapeOutside: ShapeOutsideInfo? = null,
)
