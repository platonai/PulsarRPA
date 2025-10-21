package ai.platon.cdt.kt.protocol.events.performancetimeline

import ai.platon.cdt.kt.protocol.types.performancetimeline.TimelineEvent
import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Sent when a performance timeline event is added. See reportPerformanceTimeline method.
 */
public data class TimelineEventAdded(
  @JsonProperty("event")
  public val event: TimelineEvent,
)
