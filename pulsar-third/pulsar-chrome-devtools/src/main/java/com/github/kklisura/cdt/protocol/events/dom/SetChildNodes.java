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
import java.util.List;

/**
 * Fired when backend wants to provide client with the missing DOM structure. This happens upon most
 * of the calls requesting node ids.
 */
public class SetChildNodes {

  private Integer parentId;

  private List<Node> nodes;

  /** Parent node id to populate with children. */
  public Integer getParentId() {
    return parentId;
  }

  /** Parent node id to populate with children. */
  public void setParentId(Integer parentId) {
    this.parentId = parentId;
  }

  /** Child nodes array. */
  public List<Node> getNodes() {
    return nodes;
  }

  /** Child nodes array. */
  public void setNodes(List<Node> nodes) {
    this.nodes = nodes;
  }
}
