@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.css

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String
import kotlin.collections.List

/**
 * Properties of a web font: https://www.w3.org/TR/2008/REC-CSS2-20080411/fonts.html#font-descriptions
 * and additional information such as platformFontFamily and fontVariationAxes.
 */
data class FontFace(
  @param:JsonProperty("fontFamily")
  val fontFamily: String,
  @param:JsonProperty("fontStyle")
  val fontStyle: String,
  @param:JsonProperty("fontVariant")
  val fontVariant: String,
  @param:JsonProperty("fontWeight")
  val fontWeight: String,
  @param:JsonProperty("fontStretch")
  val fontStretch: String,
  @param:JsonProperty("unicodeRange")
  val unicodeRange: String,
  @param:JsonProperty("src")
  val src: String,
  @param:JsonProperty("platformFontFamily")
  val platformFontFamily: String,
  @param:JsonProperty("fontVariationAxes")
  @param:Optional
  val fontVariationAxes: List<FontVariationAxis>? = null,
)
