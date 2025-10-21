package ai.platon.cdt.kt.protocol.types.emulation

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int

/**
 * Screen orientation.
 */
public data class ScreenOrientation(
  @JsonProperty("type")
  public val type: ScreenOrientationType,
  @JsonProperty("angle")
  public val angle: Int,
)
