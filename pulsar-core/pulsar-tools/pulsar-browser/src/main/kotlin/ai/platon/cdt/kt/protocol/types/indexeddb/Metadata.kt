package ai.platon.cdt.kt.protocol.types.indexeddb

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double

public data class Metadata(
  @JsonProperty("entriesCount")
  public val entriesCount: Double,
  @JsonProperty("keyGeneratorValue")
  public val keyGeneratorValue: Double,
)
