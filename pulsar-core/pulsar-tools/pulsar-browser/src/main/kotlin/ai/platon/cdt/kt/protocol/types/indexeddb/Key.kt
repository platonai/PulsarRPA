@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.indexeddb

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.String
import kotlin.collections.List

/**
 * Key.
 */
data class Key(
  @param:JsonProperty("type")
  val type: KeyType,
  @param:JsonProperty("number")
  @param:Optional
  val number: Double? = null,
  @param:JsonProperty("string")
  @param:Optional
  val string: String? = null,
  @param:JsonProperty("date")
  @param:Optional
  val date: Double? = null,
  @param:JsonProperty("array")
  @param:Optional
  val array: List<Key>? = null,
)
