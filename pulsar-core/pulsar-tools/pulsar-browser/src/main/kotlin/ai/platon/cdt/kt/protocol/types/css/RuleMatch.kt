package ai.platon.cdt.kt.protocol.types.css

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int
import kotlin.collections.List

/**
 * Match data for a CSS rule.
 */
public data class RuleMatch(
  @JsonProperty("rule")
  public val rule: CSSRule,
  @JsonProperty("matchingSelectors")
  public val matchingSelectors: List<Int>,
)
