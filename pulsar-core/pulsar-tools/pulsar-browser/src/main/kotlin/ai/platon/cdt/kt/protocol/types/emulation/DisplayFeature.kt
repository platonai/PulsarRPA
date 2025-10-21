package ai.platon.cdt.kt.protocol.types.emulation

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int

public data class DisplayFeature(
  @JsonProperty("orientation")
  public val orientation: DisplayFeatureOrientation,
  @JsonProperty("offset")
  public val offset: Int,
  @JsonProperty("maskLength")
  public val maskLength: Int,
)
