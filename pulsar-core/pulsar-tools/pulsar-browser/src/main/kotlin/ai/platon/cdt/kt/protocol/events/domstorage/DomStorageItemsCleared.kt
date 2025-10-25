@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.domstorage

import ai.platon.cdt.kt.protocol.types.domstorage.StorageId
import com.fasterxml.jackson.`annotation`.JsonProperty

data class DomStorageItemsCleared(
  @param:JsonProperty("storageId")
  val storageId: StorageId,
)
