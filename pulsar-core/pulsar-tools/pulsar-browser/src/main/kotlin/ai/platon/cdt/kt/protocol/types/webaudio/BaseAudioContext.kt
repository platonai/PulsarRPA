package ai.platon.cdt.kt.protocol.types.webaudio

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.String

/**
 * Protocol object for BaseAudioContext
 */
public data class BaseAudioContext(
  @JsonProperty("contextId")
  public val contextId: String,
  @JsonProperty("contextType")
  public val contextType: ContextType,
  @JsonProperty("contextState")
  public val contextState: ContextState,
  @JsonProperty("realtimeData")
  @Optional
  public val realtimeData: ContextRealtimeData? = null,
  @JsonProperty("callbackBufferSize")
  public val callbackBufferSize: Double,
  @JsonProperty("maxOutputChannelCount")
  public val maxOutputChannelCount: Double,
  @JsonProperty("sampleRate")
  public val sampleRate: Double,
)
