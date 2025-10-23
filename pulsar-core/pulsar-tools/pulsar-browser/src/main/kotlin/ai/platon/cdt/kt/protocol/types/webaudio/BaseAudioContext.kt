@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.webaudio

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.String

/**
 * Protocol object for BaseAudioContext
 */
data class BaseAudioContext(
  @param:JsonProperty("contextId")
  val contextId: String,
  @param:JsonProperty("contextType")
  val contextType: ContextType,
  @param:JsonProperty("contextState")
  val contextState: ContextState,
  @param:JsonProperty("realtimeData")
  @param:Optional
  val realtimeData: ContextRealtimeData? = null,
  @param:JsonProperty("callbackBufferSize")
  val callbackBufferSize: Double,
  @param:JsonProperty("maxOutputChannelCount")
  val maxOutputChannelCount: Double,
  @param:JsonProperty("sampleRate")
  val sampleRate: Double,
)
