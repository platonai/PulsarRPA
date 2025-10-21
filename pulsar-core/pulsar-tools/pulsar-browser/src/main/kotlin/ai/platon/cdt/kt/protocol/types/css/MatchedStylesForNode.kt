package ai.platon.cdt.kt.protocol.types.css

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.collections.List

public data class MatchedStylesForNode(
  @JsonProperty("inlineStyle")
  @Optional
  public val inlineStyle: CSSStyle? = null,
  @JsonProperty("attributesStyle")
  @Optional
  public val attributesStyle: CSSStyle? = null,
  @JsonProperty("matchedCSSRules")
  @Optional
  public val matchedCSSRules: List<RuleMatch>? = null,
  @JsonProperty("pseudoElements")
  @Optional
  public val pseudoElements: List<PseudoElementMatches>? = null,
  @JsonProperty("inherited")
  @Optional
  public val inherited: List<InheritedStyleEntry>? = null,
  @JsonProperty("cssKeyframesRules")
  @Optional
  public val cssKeyframesRules: List<CSSKeyframesRule>? = null,
)
