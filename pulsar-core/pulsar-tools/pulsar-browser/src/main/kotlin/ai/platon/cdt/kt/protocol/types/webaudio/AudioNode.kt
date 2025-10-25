@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.webaudio

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.String

/**
 * Protocol object for AudioNode
 */
data class AudioNode(
  @param:JsonProperty("nodeId")
  val nodeId: String,
  @param:JsonProperty("contextId")
  val contextId: String,
  @param:JsonProperty("nodeType")
  val nodeType: String,
  @param:JsonProperty("numberOfInputs")
  val numberOfInputs: Double,
  @param:JsonProperty("numberOfOutputs")
  val numberOfOutputs: Double,
  @param:JsonProperty("channelCount")
  val channelCount: Double,
  @param:JsonProperty("channelCountMode")
  val channelCountMode: ChannelCountMode,
  @param:JsonProperty("channelInterpretation")
  val channelInterpretation: ChannelInterpretation,
)
