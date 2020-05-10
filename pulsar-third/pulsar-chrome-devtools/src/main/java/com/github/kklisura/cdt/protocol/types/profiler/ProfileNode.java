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
import com.github.kklisura.cdt.protocol.types.runtime.CallFrame;
import java.util.List;

/** Profile node. Holds callsite information, execution statistics and child nodes. */
public class ProfileNode {

  private Integer id;

  private CallFrame callFrame;

  @Optional private Integer hitCount;

  @Optional private List<Integer> children;

  @Optional private String deoptReason;

  @Optional private List<PositionTickInfo> positionTicks;

  /** Unique id of the node. */
  public Integer getId() {
    return id;
  }

  /** Unique id of the node. */
  public void setId(Integer id) {
    this.id = id;
  }

  /** Function location. */
  public CallFrame getCallFrame() {
    return callFrame;
  }

  /** Function location. */
  public void setCallFrame(CallFrame callFrame) {
    this.callFrame = callFrame;
  }

  /** Number of samples where this node was on top of the call stack. */
  public Integer getHitCount() {
    return hitCount;
  }

  /** Number of samples where this node was on top of the call stack. */
  public void setHitCount(Integer hitCount) {
    this.hitCount = hitCount;
  }

  /** Child node ids. */
  public List<Integer> getChildren() {
    return children;
  }

  /** Child node ids. */
  public void setChildren(List<Integer> children) {
    this.children = children;
  }

  /**
   * The reason of being not optimized. The function may be deoptimized or marked as don't optimize.
   */
  public String getDeoptReason() {
    return deoptReason;
  }

  /**
   * The reason of being not optimized. The function may be deoptimized or marked as don't optimize.
   */
  public void setDeoptReason(String deoptReason) {
    this.deoptReason = deoptReason;
  }

  /** An array of source position ticks. */
  public List<PositionTickInfo> getPositionTicks() {
    return positionTicks;
  }

  /** An array of source position ticks. */
  public void setPositionTicks(List<PositionTickInfo> positionTicks) {
    this.positionTicks = positionTicks;
  }
}
