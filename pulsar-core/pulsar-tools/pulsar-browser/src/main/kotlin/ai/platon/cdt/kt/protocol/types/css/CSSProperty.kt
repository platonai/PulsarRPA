@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.css

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.String

/**
 * CSS property declaration data.
 */
data class CSSProperty(
  @param:JsonProperty("name")
  val name: String,
  @param:JsonProperty("value")
  val `value`: String,
  @param:JsonProperty("important")
  @param:Optional
  val important: Boolean? = null,
  @param:JsonProperty("implicit")
  @param:Optional
  val implicit: Boolean? = null,
  @param:JsonProperty("text")
  @param:Optional
  val text: String? = null,
  @param:JsonProperty("parsedOk")
  @param:Optional
  val parsedOk: Boolean? = null,
  @param:JsonProperty("disabled")
  @param:Optional
  val disabled: Boolean? = null,
  @param:JsonProperty("range")
  @param:Optional
  val range: SourceRange? = null,
)
