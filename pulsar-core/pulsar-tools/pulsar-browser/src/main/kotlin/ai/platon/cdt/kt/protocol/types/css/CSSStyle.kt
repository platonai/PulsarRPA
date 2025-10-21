package ai.platon.cdt.kt.protocol.types.css

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String
import kotlin.collections.List

/**
 * CSS style representation.
 */
public data class CSSStyle(
  @JsonProperty("styleSheetId")
  @Optional
  public val styleSheetId: String? = null,
  @JsonProperty("cssProperties")
  public val cssProperties: List<CSSProperty>,
  @JsonProperty("shorthandEntries")
  public val shorthandEntries: List<ShorthandEntry>,
  @JsonProperty("cssText")
  @Optional
  public val cssText: String? = null,
  @JsonProperty("range")
  @Optional
  public val range: SourceRange? = null,
)
