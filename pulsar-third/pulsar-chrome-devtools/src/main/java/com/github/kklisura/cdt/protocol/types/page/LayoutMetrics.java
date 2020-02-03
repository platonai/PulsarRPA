package com.github.kklisura.cdt.protocol.types.page;

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

import com.github.kklisura.cdt.protocol.types.dom.Rect;

public class LayoutMetrics {

  private LayoutViewport layoutViewport;

  private VisualViewport visualViewport;

  private Rect contentSize;

  /** Metrics relating to the layout viewport. */
  public LayoutViewport getLayoutViewport() {
    return layoutViewport;
  }

  /** Metrics relating to the layout viewport. */
  public void setLayoutViewport(LayoutViewport layoutViewport) {
    this.layoutViewport = layoutViewport;
  }

  /** Metrics relating to the visual viewport. */
  public VisualViewport getVisualViewport() {
    return visualViewport;
  }

  /** Metrics relating to the visual viewport. */
  public void setVisualViewport(VisualViewport visualViewport) {
    this.visualViewport = visualViewport;
  }

  /** Size of scrollable area. */
  public Rect getContentSize() {
    return contentSize;
  }

  /** Size of scrollable area. */
  public void setContentSize(Rect contentSize) {
    this.contentSize = contentSize;
  }
}
