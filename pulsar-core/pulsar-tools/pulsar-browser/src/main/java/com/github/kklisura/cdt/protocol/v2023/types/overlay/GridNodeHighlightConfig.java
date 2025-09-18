package com.github.kklisura.cdt.protocol.v2023.types.overlay;

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

/** Configurations for Persistent Grid Highlight */
public class GridNodeHighlightConfig {

  private GridHighlightConfig gridHighlightConfig;

  private Integer nodeId;

  /** A descriptor for the highlight appearance. */
  public GridHighlightConfig getGridHighlightConfig() {
    return gridHighlightConfig;
  }

  /** A descriptor for the highlight appearance. */
  public void setGridHighlightConfig(GridHighlightConfig gridHighlightConfig) {
    this.gridHighlightConfig = gridHighlightConfig;
  }

  /** Identifier of the node to highlight. */
  public Integer getNodeId() {
    return nodeId;
  }

  /** Identifier of the node to highlight. */
  public void setNodeId(Integer nodeId) {
    this.nodeId = nodeId;
  }
}
