package ai.platon.cdt.kt.protocol.types.dom

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

public data class CSSComputedStyleProperty(
  @JsonProperty("name")
  public val name: String,
  @JsonProperty("value")
  public val `value`: String,
)
