package com.github.kklisura.cdt.protocol.v2023.events.accessibility;

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
import com.github.kklisura.cdt.protocol.v2023.types.accessibility.AXNode;

import java.util.List;

/**
 * The nodesUpdated event is sent every time a previously requested node has changed the in tree.
 */
@Experimental
public class NodesUpdated {

  private List<AXNode> nodes;

  /** Updated node data. */
  public List<AXNode> getNodes() {
    return nodes;
  }

  /** Updated node data. */
  public void setNodes(List<AXNode> nodes) {
    this.nodes = nodes;
  }
}
