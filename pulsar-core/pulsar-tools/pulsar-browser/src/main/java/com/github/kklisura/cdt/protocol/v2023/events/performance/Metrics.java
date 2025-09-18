package com.github.kklisura.cdt.protocol.v2023.events.performance;

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

import com.github.kklisura.cdt.protocol.v2023.types.performance.Metric;

import java.util.List;

/** Current values of the metrics. */
public class Metrics {

  private List<Metric> metrics;

  private String title;

  /** Current values of the metrics. */
  public List<Metric> getMetrics() {
    return metrics;
  }

  /** Current values of the metrics. */
  public void setMetrics(List<Metric> metrics) {
    this.metrics = metrics;
  }

  /** Timestamp title. */
  public String getTitle() {
    return title;
  }

  /** Timestamp title. */
  public void setTitle(String title) {
    this.title = title;
  }
}
