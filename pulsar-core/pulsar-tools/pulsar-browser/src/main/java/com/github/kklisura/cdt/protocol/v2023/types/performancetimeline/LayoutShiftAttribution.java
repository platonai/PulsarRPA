package com.github.kklisura.cdt.protocol.v2023.types.performancetimeline;

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

import com.github.kklisura.cdt.protocol.v2023.support.annotations.Optional;
import com.github.kklisura.cdt.protocol.v2023.types.dom.Rect;

public class LayoutShiftAttribution {

  private Rect previousRect;

  private Rect currentRect;

  @Optional
  private Integer nodeId;

  public Rect getPreviousRect() {
    return previousRect;
  }

  public void setPreviousRect(Rect previousRect) {
    this.previousRect = previousRect;
  }

  public Rect getCurrentRect() {
    return currentRect;
  }

  public void setCurrentRect(Rect currentRect) {
    this.currentRect = currentRect;
  }

  public Integer getNodeId() {
    return nodeId;
  }

  public void setNodeId(Integer nodeId) {
    this.nodeId = nodeId;
  }
}
