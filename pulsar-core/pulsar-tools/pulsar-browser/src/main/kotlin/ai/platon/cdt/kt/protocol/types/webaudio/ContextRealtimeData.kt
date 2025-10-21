package ai.platon.cdt.kt.protocol.types.webaudio

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double

/**
 * Fields in AudioContext that change in real-time.
 */
public data class ContextRealtimeData(
  @JsonProperty("currentTime")
  public val currentTime: Double,
  @JsonProperty("renderCapacity")
  public val renderCapacity: Double,
  @JsonProperty("callbackIntervalMean")
  public val callbackIntervalMean: Double,
  @JsonProperty("callbackIntervalVariance")
  public val callbackIntervalVariance: Double,
)
