@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.domsnapshot

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.Int
import kotlin.collections.List

/**
 * Table of details of the post layout rendered text positions. The exact layout should not be regarded as
 * stable and may change between versions.
 */
data class TextBoxSnapshot(
  @param:JsonProperty("layoutIndex")
  val layoutIndex: List<Int>,
  @param:JsonProperty("bounds")
  val bounds: List<List<Double>>,
  @param:JsonProperty("start")
  val start: List<Int>,
  @param:JsonProperty("length")
  val length: List<Int>,
)
