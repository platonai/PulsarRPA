@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.css

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String
import kotlin.collections.List

/**
 * CSS style representation.
 */
data class CSSStyle(
  @param:JsonProperty("styleSheetId")
  @param:Optional
  val styleSheetId: String? = null,
  @param:JsonProperty("cssProperties")
  val cssProperties: List<CSSProperty>,
  @param:JsonProperty("shorthandEntries")
  val shorthandEntries: List<ShorthandEntry>,
  @param:JsonProperty("cssText")
  @param:Optional
  val cssText: String? = null,
  @param:JsonProperty("range")
  @param:Optional
  val range: SourceRange? = null,
)
