package ai.platon.cdt.kt.protocol.types.css

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.collections.List

/**
 * Media query descriptor.
 */
public data class MediaQuery(
  @JsonProperty("expressions")
  public val expressions: List<MediaQueryExpression>,
  @JsonProperty("active")
  public val active: Boolean,
)
