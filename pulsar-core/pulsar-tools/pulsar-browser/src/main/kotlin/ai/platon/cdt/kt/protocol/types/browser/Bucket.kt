@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.browser

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int

/**
 * Chrome histogram bucket.
 */
@Experimental
data class Bucket(
  @param:JsonProperty("low")
  val low: Int,
  @param:JsonProperty("high")
  val high: Int,
  @param:JsonProperty("count")
  val count: Int,
)
