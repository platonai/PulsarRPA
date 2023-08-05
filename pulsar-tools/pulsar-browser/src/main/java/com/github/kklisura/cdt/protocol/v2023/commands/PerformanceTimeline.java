package com.github.kklisura.cdt.protocol.v2023.commands;

/*-
 * #%L
 * cdt-java-client
 * %%
 * Copyright (C) 2018 - 2023 Kenan Klisura
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.github.kklisura.cdt.protocol.v2023.events.performancetimeline.TimelineEventAdded;
import com.github.kklisura.cdt.protocol.v2023.support.annotations.EventName;
import com.github.kklisura.cdt.protocol.v2023.support.annotations.Experimental;
import com.github.kklisura.cdt.protocol.v2023.support.annotations.ParamName;
import com.github.kklisura.cdt.protocol.v2023.support.types.EventHandler;
import com.github.kklisura.cdt.protocol.v2023.support.types.EventListener;

import java.util.List;

/**
 * Reporting of performance timeline events, as specified in
 * https://w3c.github.io/performance-timeline/#dom-performanceobserver.
 */
@Experimental
public interface PerformanceTimeline {

  /**
   * Previously buffered events would be reported before method returns. See also:
   * timelineEventAdded
   *
   * @param eventTypes The types of event to report, as specified in
   *     https://w3c.github.io/performance-timeline/#dom-performanceentry-entrytype The specified
   *     filter overrides any previous filters, passing empty filter disables recording. Note that
   *     not all types exposed to the web platform are currently supported.
   */
  void enable(@ParamName("eventTypes") List<String> eventTypes);

  /** Sent when a performance timeline event is added. See reportPerformanceTimeline method. */
  @EventName("timelineEventAdded")
  EventListener onTimelineEventAdded(EventHandler<TimelineEventAdded> eventListener);
}
