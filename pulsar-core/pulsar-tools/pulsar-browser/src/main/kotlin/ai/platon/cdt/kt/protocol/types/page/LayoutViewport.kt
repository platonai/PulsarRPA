package ai.platon.cdt.kt.protocol.types.page

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int

/**
 * Layout viewport position and dimensions.
 */
public data class LayoutViewport(
  @JsonProperty("pageX")
  public val pageX: Int,
  @JsonProperty("pageY")
  public val pageY: Int,
  @JsonProperty("clientWidth")
  public val clientWidth: Int,
  @JsonProperty("clientHeight")
  public val clientHeight: Int,
)
