package ai.platon.cdt.kt.protocol.types.browser

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int

/**
 * Browser window bounds information
 */
@Experimental
public data class Bounds(
  @JsonProperty("left")
  @Optional
  public val left: Int? = null,
  @JsonProperty("top")
  @Optional
  public val top: Int? = null,
  @JsonProperty("width")
  @Optional
  public val width: Int? = null,
  @JsonProperty("height")
  @Optional
  public val height: Int? = null,
  @JsonProperty("windowState")
  @Optional
  public val windowState: WindowState? = null,
)
