package ai.platon.cdt.kt.protocol.types.css

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String
import kotlin.collections.List

/**
 * CSS rule representation.
 */
public data class CSSRule(
  @JsonProperty("styleSheetId")
  @Optional
  public val styleSheetId: String? = null,
  @JsonProperty("selectorList")
  public val selectorList: SelectorList,
  @JsonProperty("origin")
  public val origin: StyleSheetOrigin,
  @JsonProperty("style")
  public val style: CSSStyle,
  @JsonProperty("media")
  @Optional
  public val media: List<CSSMedia>? = null,
)
