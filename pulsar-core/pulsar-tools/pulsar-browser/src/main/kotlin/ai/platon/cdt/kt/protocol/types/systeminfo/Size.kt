package ai.platon.cdt.kt.protocol.types.systeminfo

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int

/**
 * Describes the width and height dimensions of an entity.
 */
public data class Size(
  @JsonProperty("width")
  public val width: Int,
  @JsonProperty("height")
  public val height: Int,
)
