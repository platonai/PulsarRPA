package com.github.kklisura.cdt.protocol.events.tracing;

/*-
 * #%L
 * cdt-java-client
 * %%
 * Copyright (C) 2018 - 2019 Kenan Klisura
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

import com.github.kklisura.cdt.protocol.support.annotations.Optional;

public class BufferUsage {

  @Optional private Double percentFull;

  @Optional private Double eventCount;

  @Optional private Double value;

  /**
   * A number in range [0..1] that indicates the used size of event buffer as a fraction of its
   * total size.
   */
  public Double getPercentFull() {
    return percentFull;
  }

  /**
   * A number in range [0..1] that indicates the used size of event buffer as a fraction of its
   * total size.
   */
  public void setPercentFull(Double percentFull) {
    this.percentFull = percentFull;
  }

  /** An approximate number of events in the trace log. */
  public Double getEventCount() {
    return eventCount;
  }

  /** An approximate number of events in the trace log. */
  public void setEventCount(Double eventCount) {
    this.eventCount = eventCount;
  }

  /**
   * A number in range [0..1] that indicates the used size of event buffer as a fraction of its
   * total size.
   */
  public Double getValue() {
    return value;
  }

  /**
   * A number in range [0..1] that indicates the used size of event buffer as a fraction of its
   * total size.
   */
  public void setValue(Double value) {
    this.value = value;
  }
}
