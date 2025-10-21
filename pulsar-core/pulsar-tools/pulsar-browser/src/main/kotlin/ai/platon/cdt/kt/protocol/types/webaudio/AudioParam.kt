package ai.platon.cdt.kt.protocol.types.webaudio

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.String

/**
 * Protocol object for AudioParam
 */
public data class AudioParam(
  @JsonProperty("paramId")
  public val paramId: String,
  @JsonProperty("nodeId")
  public val nodeId: String,
  @JsonProperty("contextId")
  public val contextId: String,
  @JsonProperty("paramType")
  public val paramType: String,
  @JsonProperty("rate")
  public val rate: AutomationRate,
  @JsonProperty("defaultValue")
  public val defaultValue: Double,
  @JsonProperty("minValue")
  public val minValue: Double,
  @JsonProperty("maxValue")
  public val maxValue: Double,
)
