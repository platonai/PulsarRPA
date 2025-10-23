@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.commands

import ai.platon.cdt.kt.protocol.events.performancetimeline.TimelineEventAdded
import ai.platon.cdt.kt.protocol.support.annotations.EventName
import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.ParamName
import ai.platon.cdt.kt.protocol.support.types.EventHandler
import ai.platon.cdt.kt.protocol.support.types.EventListener
import kotlin.String
import kotlin.Unit
import kotlin.collections.List

/**
 * Reporting of performance timeline events, as specified in
 * https://w3c.github.io/performance-timeline/#dom-performanceobserver.
 */
@Experimental
interface PerformanceTimeline {
  /**
   * Previously buffered events would be reported before method returns.
   * See also: timelineEventAdded
   * @param eventTypes The types of event to report, as specified in
   * https://w3c.github.io/performance-timeline/#dom-performanceentry-entrytype
   * The specified filter overrides any previous filters, passing empty
   * filter disables recording.
   * Note that not all types exposed to the web platform are currently supported.
   */
  suspend fun enable(@ParamName("eventTypes") eventTypes: List<String>)

  @EventName("timelineEventAdded")
  fun onTimelineEventAdded(eventListener: EventHandler<TimelineEventAdded>): EventListener

  @EventName("timelineEventAdded")
  fun onTimelineEventAdded(eventListener: suspend (TimelineEventAdded) -> Unit): EventListener
}
