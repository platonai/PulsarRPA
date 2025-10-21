package ai.platon.cdt.kt.protocol.events.domstorage

import ai.platon.cdt.kt.protocol.types.domstorage.StorageId
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

public data class DomStorageItemRemoved(
  @JsonProperty("storageId")
  public val storageId: StorageId,
  @JsonProperty("key")
  public val key: String,
)
