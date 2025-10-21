package ai.platon.cdt.kt.protocol.types.css

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String
import kotlin.collections.List

/**
 * Selector list data.
 */
public data class SelectorList(
  @JsonProperty("selectors")
  public val selectors: List<Value>,
  @JsonProperty("text")
  public val text: String,
)
