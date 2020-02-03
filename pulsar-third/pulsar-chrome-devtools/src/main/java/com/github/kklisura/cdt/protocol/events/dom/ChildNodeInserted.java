package com.github.kklisura.cdt.protocol.events.dom;

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

import com.github.kklisura.cdt.protocol.types.dom.Node;

/** Mirrors `DOMNodeInserted` event. */
public class ChildNodeInserted {

  private Integer parentNodeId;

  private Integer previousNodeId;

  private Node node;

  /** Id of the node that has changed. */
  public Integer getParentNodeId() {
    return parentNodeId;
  }

  /** Id of the node that has changed. */
  public void setParentNodeId(Integer parentNodeId) {
    this.parentNodeId = parentNodeId;
  }

  /** If of the previous siblint. */
  public Integer getPreviousNodeId() {
    return previousNodeId;
  }

  /** If of the previous siblint. */
  public void setPreviousNodeId(Integer previousNodeId) {
    this.previousNodeId = previousNodeId;
  }

  /** Inserted node data. */
  public Node getNode() {
    return node;
  }

  /** Inserted node data. */
  public void setNode(Node node) {
    this.node = node;
  }
}
