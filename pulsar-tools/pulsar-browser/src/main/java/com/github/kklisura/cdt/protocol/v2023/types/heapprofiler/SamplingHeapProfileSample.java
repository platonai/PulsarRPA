package com.github.kklisura.cdt.protocol.v2023.types.heapprofiler;

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

/** A single sample from a sampling profile. */
public class SamplingHeapProfileSample {

  private Double size;

  private Integer nodeId;

  private Double ordinal;

  /** Allocation size in bytes attributed to the sample. */
  public Double getSize() {
    return size;
  }

  /** Allocation size in bytes attributed to the sample. */
  public void setSize(Double size) {
    this.size = size;
  }

  /** Id of the corresponding profile tree node. */
  public Integer getNodeId() {
    return nodeId;
  }

  /** Id of the corresponding profile tree node. */
  public void setNodeId(Integer nodeId) {
    this.nodeId = nodeId;
  }

  /**
   * Time-ordered sample ordinal number. It is unique across all profiles retrieved between
   * startSampling and stopSampling.
   */
  public Double getOrdinal() {
    return ordinal;
  }

  /**
   * Time-ordered sample ordinal number. It is unique across all profiles retrieved between
   * startSampling and stopSampling.
   */
  public void setOrdinal(Double ordinal) {
    this.ordinal = ordinal;
  }
}
