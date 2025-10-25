@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.indexeddb

import ai.platon.cdt.kt.protocol.types.runtime.RemoteObject
import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Data entry.
 */
data class DataEntry(
  @param:JsonProperty("key")
  val key: RemoteObject,
  @param:JsonProperty("primaryKey")
  val primaryKey: RemoteObject,
  @param:JsonProperty("value")
  val `value`: RemoteObject,
)
