package ai.platon.cdt.kt.protocol.types.css

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.String

public data class ShorthandEntry(
  @JsonProperty("name")
  public val name: String,
  @JsonProperty("value")
  public val `value`: String,
  @JsonProperty("important")
  @Optional
  public val important: Boolean? = null,
)
