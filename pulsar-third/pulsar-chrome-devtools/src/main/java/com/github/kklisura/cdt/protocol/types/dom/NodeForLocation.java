package com.github.kklisura.cdt.protocol.types.dom;

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

public class NodeForLocation {

  private Integer backendNodeId;

  @Optional private Integer nodeId;

  /** Resulting node. */
  public Integer getBackendNodeId() {
    return backendNodeId;
  }

  /** Resulting node. */
  public void setBackendNodeId(Integer backendNodeId) {
    this.backendNodeId = backendNodeId;
  }

  /** Id of the node at given coordinates, only when enabled and requested document. */
  public Integer getNodeId() {
    return nodeId;
  }

  /** Id of the node at given coordinates, only when enabled and requested document. */
  public void setNodeId(Integer nodeId) {
    this.nodeId = nodeId;
  }
}
