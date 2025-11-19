@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.css

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String
import kotlin.collections.List

/**
 * CSS rule representation.
 */
data class CSSRule(
  @param:JsonProperty("styleSheetId")
  @param:Optional
  val styleSheetId: String? = null,
  @param:JsonProperty("selectorList")
  val selectorList: SelectorList,
  @param:JsonProperty("origin")
  val origin: StyleSheetOrigin,
  @param:JsonProperty("style")
  val style: CSSStyle,
  @param:JsonProperty("media")
  @param:Optional
  val media: List<CSSMedia>? = null,
)
