package ai.platon.cdt.kt.protocol.types.domsnapshot

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int
import kotlin.collections.List

public data class RareBooleanData(
  @JsonProperty("index")
  public val index: List<Int>,
)
