@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.heapprofiler

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.Int

/**
 * If heap objects tracking has been started then backend regularly sends a current value for last
 * seen object id and corresponding timestamp. If the were changes in the heap since last event
 * then one or more heapStatsUpdate events will be sent before a new lastSeenObjectId event.
 */
data class LastSeenObjectId(
  @param:JsonProperty("lastSeenObjectId")
  val lastSeenObjectId: Int,
  @param:JsonProperty("timestamp")
  val timestamp: Double,
)
