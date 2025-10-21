package ai.platon.cdt.kt.protocol.types.emulation

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

public data class MediaFeature(
  @JsonProperty("name")
  public val name: String,
  @JsonProperty("value")
  public val `value`: String,
)
