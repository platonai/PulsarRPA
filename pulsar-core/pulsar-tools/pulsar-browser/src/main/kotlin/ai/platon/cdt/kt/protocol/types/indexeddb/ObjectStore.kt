@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.indexeddb

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.String
import kotlin.collections.List

/**
 * Object store.
 */
data class ObjectStore(
  @param:JsonProperty("name")
  val name: String,
  @param:JsonProperty("keyPath")
  val keyPath: KeyPath,
  @param:JsonProperty("autoIncrement")
  val autoIncrement: Boolean,
  @param:JsonProperty("indexes")
  val indexes: List<ObjectStoreIndex>,
)
