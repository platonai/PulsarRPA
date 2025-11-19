@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.css

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * A descriptor of operation to mutate style declaration text.
 */
data class StyleDeclarationEdit(
  @param:JsonProperty("styleSheetId")
  val styleSheetId: String,
  @param:JsonProperty("range")
  val range: SourceRange,
  @param:JsonProperty("text")
  val text: String,
)
