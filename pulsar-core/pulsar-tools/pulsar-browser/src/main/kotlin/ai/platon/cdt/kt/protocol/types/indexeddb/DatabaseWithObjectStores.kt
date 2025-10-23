@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.indexeddb

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.String
import kotlin.collections.List

/**
 * Database with an array of object stores.
 */
data class DatabaseWithObjectStores(
  @param:JsonProperty("name")
  val name: String,
  @param:JsonProperty("version")
  val version: Double,
  @param:JsonProperty("objectStores")
  val objectStores: List<ObjectStore>,
)
