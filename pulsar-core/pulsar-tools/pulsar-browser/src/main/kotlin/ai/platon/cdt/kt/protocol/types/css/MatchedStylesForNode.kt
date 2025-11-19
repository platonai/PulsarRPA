@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.css

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.collections.List

data class MatchedStylesForNode(
  @param:JsonProperty("inlineStyle")
  @param:Optional
  val inlineStyle: CSSStyle? = null,
  @param:JsonProperty("attributesStyle")
  @param:Optional
  val attributesStyle: CSSStyle? = null,
  @param:JsonProperty("matchedCSSRules")
  @param:Optional
  val matchedCSSRules: List<RuleMatch>? = null,
  @param:JsonProperty("pseudoElements")
  @param:Optional
  val pseudoElements: List<PseudoElementMatches>? = null,
  @param:JsonProperty("inherited")
  @param:Optional
  val inherited: List<InheritedStyleEntry>? = null,
  @param:JsonProperty("cssKeyframesRules")
  @param:Optional
  val cssKeyframesRules: List<CSSKeyframesRule>? = null,
)
