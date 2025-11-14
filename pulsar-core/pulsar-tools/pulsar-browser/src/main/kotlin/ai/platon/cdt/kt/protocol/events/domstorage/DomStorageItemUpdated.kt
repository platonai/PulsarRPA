@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.domstorage

import ai.platon.cdt.kt.protocol.types.domstorage.StorageId
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

data class DomStorageItemUpdated(
  @param:JsonProperty("storageId")
  val storageId: StorageId,
  @param:JsonProperty("key")
  val key: String,
  @param:JsonProperty("oldValue")
  val oldValue: String,
  @param:JsonProperty("newValue")
  val newValue: String,
)
