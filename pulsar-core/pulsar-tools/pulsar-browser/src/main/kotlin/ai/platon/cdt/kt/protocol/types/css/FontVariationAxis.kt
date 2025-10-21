package ai.platon.cdt.kt.protocol.types.css

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.String

/**
 * Information about font variation axes for variable fonts
 */
public data class FontVariationAxis(
  @JsonProperty("tag")
  public val tag: String,
  @JsonProperty("name")
  public val name: String,
  @JsonProperty("minValue")
  public val minValue: Double,
  @JsonProperty("maxValue")
  public val maxValue: Double,
  @JsonProperty("defaultValue")
  public val defaultValue: Double,
)
