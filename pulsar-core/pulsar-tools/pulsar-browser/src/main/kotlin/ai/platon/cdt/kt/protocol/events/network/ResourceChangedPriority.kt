@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.network

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.types.network.ResourcePriority
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.String

/**
 * Fired when resource loading priority is changed
 */
@Experimental
data class ResourceChangedPriority(
  @param:JsonProperty("requestId")
  val requestId: String,
  @param:JsonProperty("newPriority")
  val newPriority: ResourcePriority,
  @param:JsonProperty("timestamp")
  val timestamp: Double,
)
