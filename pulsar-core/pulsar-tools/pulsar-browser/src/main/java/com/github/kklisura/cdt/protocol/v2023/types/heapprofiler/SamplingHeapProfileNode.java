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

import com.github.kklisura.cdt.protocol.v2023.types.runtime.CallFrame;

import java.util.List;

/**
 * Sampling Heap Profile node. Holds callsite information, allocation statistics and child nodes.
 */
public class SamplingHeapProfileNode {

  private CallFrame callFrame;

  private Double selfSize;

  private Integer id;

  private List<SamplingHeapProfileNode> children;

  /** Function location. */
  public CallFrame getCallFrame() {
    return callFrame;
  }

  /** Function location. */
  public void setCallFrame(CallFrame callFrame) {
    this.callFrame = callFrame;
  }

  /** Allocations size in bytes for the node excluding children. */
  public Double getSelfSize() {
    return selfSize;
  }

  /** Allocations size in bytes for the node excluding children. */
  public void setSelfSize(Double selfSize) {
    this.selfSize = selfSize;
  }

  /**
   * Node id. Ids are unique across all profiles collected between startSampling and stopSampling.
   */
  public Integer getId() {
    return id;
  }

  /**
   * Node id. Ids are unique across all profiles collected between startSampling and stopSampling.
   */
  public void setId(Integer id) {
    this.id = id;
  }

  /** Child nodes. */
  public List<SamplingHeapProfileNode> getChildren() {
    return children;
  }

  /** Child nodes. */
  public void setChildren(List<SamplingHeapProfileNode> children) {
    this.children = children;
  }
}
