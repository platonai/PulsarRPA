@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.cachestorage

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.collections.List

data class RequestEntries(
  @param:JsonProperty("cacheDataEntries")
  val cacheDataEntries: List<DataEntry>,
  @param:JsonProperty("returnCount")
  val returnCount: Double,
)
