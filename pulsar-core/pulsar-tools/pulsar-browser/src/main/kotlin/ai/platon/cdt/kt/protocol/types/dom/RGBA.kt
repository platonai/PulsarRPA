package ai.platon.cdt.kt.protocol.types.dom

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.Int

/**
 * A structure holding an RGBA color.
 */
public data class RGBA(
  @JsonProperty("r")
  public val r: Int,
  @JsonProperty("g")
  public val g: Int,
  @JsonProperty("b")
  public val b: Int,
  @JsonProperty("a")
  @Optional
  public val a: Double? = null,
)
