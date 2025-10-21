package ai.platon.cdt.kt.protocol.types.indexeddb

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.collections.List

public data class RequestData(
  @JsonProperty("objectStoreDataEntries")
  public val objectStoreDataEntries: List<DataEntry>,
  @JsonProperty("hasMore")
  public val hasMore: Boolean,
)
