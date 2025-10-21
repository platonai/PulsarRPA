package ai.platon.cdt.kt.protocol.types.accessibility

import com.fasterxml.jackson.`annotation`.JsonProperty

public data class AXProperty(
  @JsonProperty("name")
  public val name: AXPropertyName,
  @JsonProperty("value")
  public val `value`: AXValue,
)
