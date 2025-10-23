@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.webaudio

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.String

/**
 * Protocol object for AudioParam
 */
data class AudioParam(
  @param:JsonProperty("paramId")
  val paramId: String,
  @param:JsonProperty("nodeId")
  val nodeId: String,
  @param:JsonProperty("contextId")
  val contextId: String,
  @param:JsonProperty("paramType")
  val paramType: String,
  @param:JsonProperty("rate")
  val rate: AutomationRate,
  @param:JsonProperty("defaultValue")
  val defaultValue: Double,
  @param:JsonProperty("minValue")
  val minValue: Double,
  @param:JsonProperty("maxValue")
  val maxValue: Double,
)
