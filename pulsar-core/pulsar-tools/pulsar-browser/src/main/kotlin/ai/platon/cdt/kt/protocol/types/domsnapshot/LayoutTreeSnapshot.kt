package ai.platon.cdt.kt.protocol.types.domsnapshot

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.Int
import kotlin.collections.List

/**
 * Table of details of an element in the DOM tree with a LayoutObject.
 */
public data class LayoutTreeSnapshot(
  @JsonProperty("nodeIndex")
  public val nodeIndex: List<Int>,
  @JsonProperty("styles")
  public val styles: List<List<Int>>,
  @JsonProperty("bounds")
  public val bounds: List<List<Double>>,
  @JsonProperty("text")
  public val text: List<Int>,
  @JsonProperty("stackingContexts")
  public val stackingContexts: RareBooleanData,
  @JsonProperty("paintOrders")
  @Optional
  public val paintOrders: List<Int>? = null,
  @JsonProperty("offsetRects")
  @Optional
  public val offsetRects: List<List<Double>>? = null,
  @JsonProperty("scrollRects")
  @Optional
  public val scrollRects: List<List<Double>>? = null,
  @JsonProperty("clientRects")
  @Optional
  public val clientRects: List<List<Double>>? = null,
  @JsonProperty("blendedBackgroundColors")
  @Optional
  @Experimental
  public val blendedBackgroundColors: List<Int>? = null,
  @JsonProperty("textColorOpacities")
  @Optional
  @Experimental
  public val textColorOpacities: List<Double>? = null,
)
