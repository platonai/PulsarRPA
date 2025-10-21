package ai.platon.cdt.kt.protocol.types.page

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int
import kotlin.collections.List

public data class NavigationHistory(
  @JsonProperty("currentIndex")
  public val currentIndex: Int,
  @JsonProperty("entries")
  public val entries: List<NavigationEntry>,
)
