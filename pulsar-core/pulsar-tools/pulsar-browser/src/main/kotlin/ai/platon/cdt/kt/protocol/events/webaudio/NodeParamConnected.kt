@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.webaudio

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.String

/**
 * Notifies that an AudioNode is connected to an AudioParam.
 */
data class NodeParamConnected(
  @param:JsonProperty("contextId")
  val contextId: String,
  @param:JsonProperty("sourceId")
  val sourceId: String,
  @param:JsonProperty("destinationId")
  val destinationId: String,
  @param:JsonProperty("sourceOutputIndex")
  @param:Optional
  val sourceOutputIndex: Double? = null,
)
