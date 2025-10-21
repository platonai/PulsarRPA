package ai.platon.cdt.kt.protocol.types.webaudio

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.String

/**
 * Protocol object for AudioNode
 */
public data class AudioNode(
  @JsonProperty("nodeId")
  public val nodeId: String,
  @JsonProperty("contextId")
  public val contextId: String,
  @JsonProperty("nodeType")
  public val nodeType: String,
  @JsonProperty("numberOfInputs")
  public val numberOfInputs: Double,
  @JsonProperty("numberOfOutputs")
  public val numberOfOutputs: Double,
  @JsonProperty("channelCount")
  public val channelCount: Double,
  @JsonProperty("channelCountMode")
  public val channelCountMode: ChannelCountMode,
  @JsonProperty("channelInterpretation")
  public val channelInterpretation: ChannelInterpretation,
)
