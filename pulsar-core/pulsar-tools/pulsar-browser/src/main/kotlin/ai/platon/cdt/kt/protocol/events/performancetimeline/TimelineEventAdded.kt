@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.performancetimeline

import ai.platon.cdt.kt.protocol.types.performancetimeline.TimelineEvent
import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Sent when a performance timeline event is added. See reportPerformanceTimeline method.
 */
data class TimelineEventAdded(
  @param:JsonProperty("event")
  val event: TimelineEvent,
)
