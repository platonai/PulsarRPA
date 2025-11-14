@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.indexeddb

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String
import kotlin.collections.List

/**
 * Key path.
 */
data class KeyPath(
  @param:JsonProperty("type")
  val type: KeyPathType,
  @param:JsonProperty("string")
  @param:Optional
  val string: String? = null,
  @param:JsonProperty("array")
  @param:Optional
  val array: List<String>? = null,
)
