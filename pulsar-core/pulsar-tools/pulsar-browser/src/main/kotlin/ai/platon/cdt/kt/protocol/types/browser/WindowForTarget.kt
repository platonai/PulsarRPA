package ai.platon.cdt.kt.protocol.types.browser

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int

public data class WindowForTarget(
  @JsonProperty("windowId")
  public val windowId: Int,
  @JsonProperty("bounds")
  public val bounds: Bounds,
)
