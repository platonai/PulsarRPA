package com.github.kklisura.cdt.protocol.v2023.events.dom;

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

/** Fired when `Container`'s child node count has changed. */
public class ChildNodeCountUpdated {

  private Integer nodeId;

  private Integer childNodeCount;

  /** Id of the node that has changed. */
  public Integer getNodeId() {
    return nodeId;
  }

  /** Id of the node that has changed. */
  public void setNodeId(Integer nodeId) {
    this.nodeId = nodeId;
  }

  /** New node count. */
  public Integer getChildNodeCount() {
    return childNodeCount;
  }

  /** New node count. */
  public void setChildNodeCount(Integer childNodeCount) {
    this.childNodeCount = childNodeCount;
  }
}
