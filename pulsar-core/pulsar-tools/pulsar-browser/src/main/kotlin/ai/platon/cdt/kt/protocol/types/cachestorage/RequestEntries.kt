package ai.platon.cdt.kt.protocol.types.cachestorage

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.collections.List

public data class RequestEntries(
  @JsonProperty("cacheDataEntries")
  public val cacheDataEntries: List<DataEntry>,
  @JsonProperty("returnCount")
  public val returnCount: Double,
)
