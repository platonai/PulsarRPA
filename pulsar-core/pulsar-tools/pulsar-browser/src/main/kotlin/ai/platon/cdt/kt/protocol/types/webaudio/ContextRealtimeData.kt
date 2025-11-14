@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.webaudio

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double

/**
 * Fields in AudioContext that change in real-time.
 */
data class ContextRealtimeData(
  @param:JsonProperty("currentTime")
  val currentTime: Double,
  @param:JsonProperty("renderCapacity")
  val renderCapacity: Double,
  @param:JsonProperty("callbackIntervalMean")
  val callbackIntervalMean: Double,
  @param:JsonProperty("callbackIntervalVariance")
  val callbackIntervalVariance: Double,
)
