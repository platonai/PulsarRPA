@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.indexeddb

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean

/**
 * Key range.
 */
data class KeyRange(
  @param:JsonProperty("lower")
  @param:Optional
  val lower: Key? = null,
  @param:JsonProperty("upper")
  @param:Optional
  val upper: Key? = null,
  @param:JsonProperty("lowerOpen")
  val lowerOpen: Boolean,
  @param:JsonProperty("upperOpen")
  val upperOpen: Boolean,
)
