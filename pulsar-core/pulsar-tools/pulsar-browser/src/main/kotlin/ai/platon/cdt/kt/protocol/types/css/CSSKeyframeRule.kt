package ai.platon.cdt.kt.protocol.types.css

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * CSS keyframe rule representation.
 */
public data class CSSKeyframeRule(
  @JsonProperty("styleSheetId")
  @Optional
  public val styleSheetId: String? = null,
  @JsonProperty("origin")
  public val origin: StyleSheetOrigin,
  @JsonProperty("keyText")
  public val keyText: Value,
  @JsonProperty("style")
  public val style: CSSStyle,
)
