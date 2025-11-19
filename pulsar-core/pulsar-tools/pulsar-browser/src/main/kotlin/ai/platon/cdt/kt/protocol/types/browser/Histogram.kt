@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.browser

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int
import kotlin.String
import kotlin.collections.List

/**
 * Chrome histogram.
 */
@Experimental
data class Histogram(
  @param:JsonProperty("name")
  val name: String,
  @param:JsonProperty("sum")
  val sum: Int,
  @param:JsonProperty("count")
  val count: Int,
  @param:JsonProperty("buckets")
  val buckets: List<Bucket>,
)
