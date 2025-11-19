@file:Suppress("unused")
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
data class LayoutTreeSnapshot(
  @param:JsonProperty("nodeIndex")
  val nodeIndex: List<Int>,
  @param:JsonProperty("styles")
  val styles: List<List<Int>>,
  @param:JsonProperty("bounds")
  val bounds: List<List<Double>>,
  @param:JsonProperty("text")
  val text: List<Int>,
  @param:JsonProperty("stackingContexts")
  val stackingContexts: RareBooleanData,
  @param:JsonProperty("paintOrders")
  @param:Optional
  val paintOrders: List<Int>? = null,
  @param:JsonProperty("offsetRects")
  @param:Optional
  val offsetRects: List<List<Double>>? = null,
  @param:JsonProperty("scrollRects")
  @param:Optional
  val scrollRects: List<List<Double>>? = null,
  @param:JsonProperty("clientRects")
  @param:Optional
  val clientRects: List<List<Double>>? = null,
  @param:JsonProperty("blendedBackgroundColors")
  @param:Optional
  @param:Experimental
  val blendedBackgroundColors: List<Int>? = null,
  @param:JsonProperty("textColorOpacities")
  @param:Optional
  @param:Experimental
  val textColorOpacities: List<Double>? = null,
)
