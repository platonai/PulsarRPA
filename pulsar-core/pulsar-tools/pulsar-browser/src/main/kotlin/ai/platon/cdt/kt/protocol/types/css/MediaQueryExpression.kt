package ai.platon.cdt.kt.protocol.types.css

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.String

/**
 * Media query expression descriptor.
 */
public data class MediaQueryExpression(
  @JsonProperty("value")
  public val `value`: Double,
  @JsonProperty("unit")
  public val unit: String,
  @JsonProperty("feature")
  public val feature: String,
  @JsonProperty("valueRange")
  @Optional
  public val valueRange: SourceRange? = null,
  @JsonProperty("computedLength")
  @Optional
  public val computedLength: Double? = null,
)
