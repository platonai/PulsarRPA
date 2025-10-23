@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.indexeddb

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.String

/**
 * Object store index.
 */
data class ObjectStoreIndex(
  @param:JsonProperty("name")
  val name: String,
  @param:JsonProperty("keyPath")
  val keyPath: KeyPath,
  @param:JsonProperty("unique")
  val unique: Boolean,
  @param:JsonProperty("multiEntry")
  val multiEntry: Boolean,
)
