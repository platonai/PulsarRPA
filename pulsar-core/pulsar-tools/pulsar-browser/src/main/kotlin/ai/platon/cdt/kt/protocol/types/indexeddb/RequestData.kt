@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.indexeddb

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.collections.List

data class RequestData(
  @param:JsonProperty("objectStoreDataEntries")
  val objectStoreDataEntries: List<DataEntry>,
  @param:JsonProperty("hasMore")
  val hasMore: Boolean,
)
