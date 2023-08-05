package com.github.kklisura.cdt.protocol.v2023.events.profiler;

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

import com.github.kklisura.cdt.protocol.v2023.support.annotations.Experimental;
import com.github.kklisura.cdt.protocol.v2023.types.profiler.ScriptCoverage;

import java.util.List;

/**
 * Reports coverage delta since the last poll (either from an event like this, or from
 * `takePreciseCoverage` for the current isolate. May only be sent if precise code coverage has been
 * started. This event can be trigged by the embedder to, for example, trigger collection of
 * coverage data immediately at a certain point in time.
 */
@Experimental
public class PreciseCoverageDeltaUpdate {

  private Double timestamp;

  private String occasion;

  private List<ScriptCoverage> result;

  /**
   * Monotonically increasing time (in seconds) when the coverage update was taken in the backend.
   */
  public Double getTimestamp() {
    return timestamp;
  }

  /**
   * Monotonically increasing time (in seconds) when the coverage update was taken in the backend.
   */
  public void setTimestamp(Double timestamp) {
    this.timestamp = timestamp;
  }

  /** Identifier for distinguishing coverage events. */
  public String getOccasion() {
    return occasion;
  }

  /** Identifier for distinguishing coverage events. */
  public void setOccasion(String occasion) {
    this.occasion = occasion;
  }

  /** Coverage data for the current isolate. */
  public List<ScriptCoverage> getResult() {
    return result;
  }

  /** Coverage data for the current isolate. */
  public void setResult(List<ScriptCoverage> result) {
    this.result = result;
  }
}
