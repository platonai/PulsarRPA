package ai.platon.cdt.kt.protocol.types.dom

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int
import kotlin.String

public data class PerformSearch(
  @JsonProperty("searchId")
  public val searchId: String,
  @JsonProperty("resultCount")
  public val resultCount: Int,
)
