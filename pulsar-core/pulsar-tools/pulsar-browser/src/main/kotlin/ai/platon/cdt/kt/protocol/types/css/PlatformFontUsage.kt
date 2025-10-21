package ai.platon.cdt.kt.protocol.types.css

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.Double
import kotlin.String

/**
 * Information about amount of glyphs that were rendered with given font.
 */
public data class PlatformFontUsage(
  @JsonProperty("familyName")
  public val familyName: String,
  @JsonProperty("isCustomFont")
  public val isCustomFont: Boolean,
  @JsonProperty("glyphCount")
  public val glyphCount: Double,
)
