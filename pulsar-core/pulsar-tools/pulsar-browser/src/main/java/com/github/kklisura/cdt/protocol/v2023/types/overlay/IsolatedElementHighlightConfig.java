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

public class IsolatedElementHighlightConfig {

  private IsolationModeHighlightConfig isolationModeHighlightConfig;

  private Integer nodeId;

  /** A descriptor for the highlight appearance of an element in isolation mode. */
  public IsolationModeHighlightConfig getIsolationModeHighlightConfig() {
    return isolationModeHighlightConfig;
  }

  /** A descriptor for the highlight appearance of an element in isolation mode. */
  public void setIsolationModeHighlightConfig(
      IsolationModeHighlightConfig isolationModeHighlightConfig) {
    this.isolationModeHighlightConfig = isolationModeHighlightConfig;
  }

  /** Identifier of the isolated element to highlight. */
  public Integer getNodeId() {
    return nodeId;
  }

  /** Identifier of the isolated element to highlight. */
  public void setNodeId(Integer nodeId) {
    this.nodeId = nodeId;
  }
}
