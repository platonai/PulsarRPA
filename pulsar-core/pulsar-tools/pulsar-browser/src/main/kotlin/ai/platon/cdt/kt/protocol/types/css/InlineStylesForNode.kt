package ai.platon.cdt.kt.protocol.types.css

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty

public data class InlineStylesForNode(
  @JsonProperty("inlineStyle")
  @Optional
  public val inlineStyle: CSSStyle? = null,
  @JsonProperty("attributesStyle")
  @Optional
  public val attributesStyle: CSSStyle? = null,
)
