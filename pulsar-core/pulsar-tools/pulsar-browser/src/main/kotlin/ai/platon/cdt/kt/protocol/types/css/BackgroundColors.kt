package ai.platon.cdt.kt.protocol.types.css

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String
import kotlin.collections.List

public data class BackgroundColors(
  @JsonProperty("backgroundColors")
  @Optional
  public val backgroundColors: List<String>? = null,
  @JsonProperty("computedFontSize")
  @Optional
  public val computedFontSize: String? = null,
  @JsonProperty("computedFontWeight")
  @Optional
  public val computedFontWeight: String? = null,
)
