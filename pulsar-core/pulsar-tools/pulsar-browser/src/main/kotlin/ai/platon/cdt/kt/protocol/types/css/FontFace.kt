package ai.platon.cdt.kt.protocol.types.css

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String
import kotlin.collections.List

/**
 * Properties of a web font:
 * https://www.w3.org/TR/2008/REC-CSS2-20080411/fonts.html#font-descriptions
 * and additional information such as platformFontFamily and fontVariationAxes.
 */
public data class FontFace(
  @JsonProperty("fontFamily")
  public val fontFamily: String,
  @JsonProperty("fontStyle")
  public val fontStyle: String,
  @JsonProperty("fontVariant")
  public val fontVariant: String,
  @JsonProperty("fontWeight")
  public val fontWeight: String,
  @JsonProperty("fontStretch")
  public val fontStretch: String,
  @JsonProperty("unicodeRange")
  public val unicodeRange: String,
  @JsonProperty("src")
  public val src: String,
  @JsonProperty("platformFontFamily")
  public val platformFontFamily: String,
  @JsonProperty("fontVariationAxes")
  @Optional
  public val fontVariationAxes: List<FontVariationAxis>? = null,
)
