package ai.platon.cdt.kt.protocol.types.css

import ai.platon.cdt.kt.protocol.types.dom.PseudoType
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.collections.List

/**
 * CSS rule collection for a single pseudo style.
 */
public data class PseudoElementMatches(
  @JsonProperty("pseudoType")
  public val pseudoType: PseudoType,
  @JsonProperty("matches")
  public val matches: List<RuleMatch>,
)
