@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.css

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.collections.List

/**
 * Inherited CSS rule collection from ancestor node.
 */
data class InheritedStyleEntry(
  @param:JsonProperty("inlineStyle")
  @param:Optional
  val inlineStyle: CSSStyle? = null,
  @param:JsonProperty("matchedCSSRules")
  val matchedCSSRules: List<RuleMatch>,
)
