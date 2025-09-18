package com.github.kklisura.cdt.protocol.v2023.types.performancetimeline;

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

import com.github.kklisura.cdt.protocol.v2023.support.annotations.Optional;

public class TimelineEvent {

  private String frameId;

  private String type;

  private String name;

  private Double time;

  @Optional
  private Double duration;

  @Optional private LargestContentfulPaint lcpDetails;

  @Optional private LayoutShift layoutShiftDetails;

  /** Identifies the frame that this event is related to. Empty for non-frame targets. */
  public String getFrameId() {
    return frameId;
  }

  /** Identifies the frame that this event is related to. Empty for non-frame targets. */
  public void setFrameId(String frameId) {
    this.frameId = frameId;
  }

  /**
   * The event type, as specified in
   * https://w3c.github.io/performance-timeline/#dom-performanceentry-entrytype This determines
   * which of the optional "details" fiedls is present.
   */
  public String getType() {
    return type;
  }

  /**
   * The event type, as specified in
   * https://w3c.github.io/performance-timeline/#dom-performanceentry-entrytype This determines
   * which of the optional "details" fiedls is present.
   */
  public void setType(String type) {
    this.type = type;
  }

  /** Name may be empty depending on the type. */
  public String getName() {
    return name;
  }

  /** Name may be empty depending on the type. */
  public void setName(String name) {
    this.name = name;
  }

  /** Time in seconds since Epoch, monotonically increasing within document lifetime. */
  public Double getTime() {
    return time;
  }

  /** Time in seconds since Epoch, monotonically increasing within document lifetime. */
  public void setTime(Double time) {
    this.time = time;
  }

  /** Event duration, if applicable. */
  public Double getDuration() {
    return duration;
  }

  /** Event duration, if applicable. */
  public void setDuration(Double duration) {
    this.duration = duration;
  }

  public LargestContentfulPaint getLcpDetails() {
    return lcpDetails;
  }

  public void setLcpDetails(LargestContentfulPaint lcpDetails) {
    this.lcpDetails = lcpDetails;
  }

  public LayoutShift getLayoutShiftDetails() {
    return layoutShiftDetails;
  }

  public void setLayoutShiftDetails(LayoutShift layoutShiftDetails) {
    this.layoutShiftDetails = layoutShiftDetails;
  }
}
