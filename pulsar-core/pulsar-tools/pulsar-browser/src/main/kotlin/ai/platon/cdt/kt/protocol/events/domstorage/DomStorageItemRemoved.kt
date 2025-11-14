@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.domstorage

import ai.platon.cdt.kt.protocol.types.domstorage.StorageId
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

data class DomStorageItemRemoved(
  @param:JsonProperty("storageId")
  val storageId: StorageId,
  @param:JsonProperty("key")
  val key: String,
)
