package com.github.kklisura.cdt.protocol.types.profiler;

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
import java.util.List;

/** Profile. */
public class Profile {

  private List<ProfileNode> nodes;

  private Double startTime;

  private Double endTime;

  @Optional private List<Integer> samples;

  @Optional private List<Integer> timeDeltas;

  /** The list of profile nodes. First item is the root node. */
  public List<ProfileNode> getNodes() {
    return nodes;
  }

  /** The list of profile nodes. First item is the root node. */
  public void setNodes(List<ProfileNode> nodes) {
    this.nodes = nodes;
  }

  /** Profiling start timestamp in microseconds. */
  public Double getStartTime() {
    return startTime;
  }

  /** Profiling start timestamp in microseconds. */
  public void setStartTime(Double startTime) {
    this.startTime = startTime;
  }

  /** Profiling end timestamp in microseconds. */
  public Double getEndTime() {
    return endTime;
  }

  /** Profiling end timestamp in microseconds. */
  public void setEndTime(Double endTime) {
    this.endTime = endTime;
  }

  /** Ids of samples top nodes. */
  public List<Integer> getSamples() {
    return samples;
  }

  /** Ids of samples top nodes. */
  public void setSamples(List<Integer> samples) {
    this.samples = samples;
  }

  /**
   * Time intervals between adjacent samples in microseconds. The first delta is relative to the
   * profile startTime.
   */
  public List<Integer> getTimeDeltas() {
    return timeDeltas;
  }

  /**
   * Time intervals between adjacent samples in microseconds. The first delta is relative to the
   * profile startTime.
   */
  public void setTimeDeltas(List<Integer> timeDeltas) {
    this.timeDeltas = timeDeltas;
  }
}
