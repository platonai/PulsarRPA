package ai.platon.cdt.kt.protocol.types.css

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.String

/**
 * CSS property declaration data.
 */
public data class CSSProperty(
  @JsonProperty("name")
  public val name: String,
  @JsonProperty("value")
  public val `value`: String,
  @JsonProperty("important")
  @Optional
  public val important: Boolean? = null,
  @JsonProperty("implicit")
  @Optional
  public val implicit: Boolean? = null,
  @JsonProperty("text")
  @Optional
  public val text: String? = null,
  @JsonProperty("parsedOk")
  @Optional
  public val parsedOk: Boolean? = null,
  @JsonProperty("disabled")
  @Optional
  public val disabled: Boolean? = null,
  @JsonProperty("range")
  @Optional
  public val range: SourceRange? = null,
)
