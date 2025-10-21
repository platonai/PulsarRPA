package ai.platon.cdt.kt.protocol.types.layertree

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.Double
import kotlin.Int
import kotlin.String
import kotlin.collections.List

/**
 * Information about a compositing layer.
 */
public data class Layer(
  @JsonProperty("layerId")
  public val layerId: String,
  @JsonProperty("parentLayerId")
  @Optional
  public val parentLayerId: String? = null,
  @JsonProperty("backendNodeId")
  @Optional
  public val backendNodeId: Int? = null,
  @JsonProperty("offsetX")
  public val offsetX: Double,
  @JsonProperty("offsetY")
  public val offsetY: Double,
  @JsonProperty("width")
  public val width: Double,
  @JsonProperty("height")
  public val height: Double,
  @JsonProperty("transform")
  @Optional
  public val transform: List<Double>? = null,
  @JsonProperty("anchorX")
  @Optional
  public val anchorX: Double? = null,
  @JsonProperty("anchorY")
  @Optional
  public val anchorY: Double? = null,
  @JsonProperty("anchorZ")
  @Optional
  public val anchorZ: Double? = null,
  @JsonProperty("paintCount")
  public val paintCount: Int,
  @JsonProperty("drawsContent")
  public val drawsContent: Boolean,
  @JsonProperty("invisible")
  @Optional
  public val invisible: Boolean? = null,
  @JsonProperty("scrollRects")
  @Optional
  public val scrollRects: List<ScrollRect>? = null,
  @JsonProperty("stickyPositionConstraint")
  @Optional
  public val stickyPositionConstraint: StickyPositionConstraint? = null,
)
