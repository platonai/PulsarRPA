package com.github.kklisura.cdt.protocol.v2023.types.page;

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

import com.github.kklisura.cdt.protocol.v2023.types.dom.Rect;

public class LayoutMetrics {

  @Deprecated private LayoutViewport layoutViewport;

  @Deprecated private VisualViewport visualViewport;

  @Deprecated private Rect contentSize;

  private LayoutViewport cssLayoutViewport;

  private VisualViewport cssVisualViewport;

  private Rect cssContentSize;

  /**
   * Deprecated metrics relating to the layout viewport. Is in device pixels. Use
   * `cssLayoutViewport` instead.
   */
  public LayoutViewport getLayoutViewport() {
    return layoutViewport;
  }

  /**
   * Deprecated metrics relating to the layout viewport. Is in device pixels. Use
   * `cssLayoutViewport` instead.
   */
  public void setLayoutViewport(LayoutViewport layoutViewport) {
    this.layoutViewport = layoutViewport;
  }

  /**
   * Deprecated metrics relating to the visual viewport. Is in device pixels. Use
   * `cssVisualViewport` instead.
   */
  public VisualViewport getVisualViewport() {
    return visualViewport;
  }

  /**
   * Deprecated metrics relating to the visual viewport. Is in device pixels. Use
   * `cssVisualViewport` instead.
   */
  public void setVisualViewport(VisualViewport visualViewport) {
    this.visualViewport = visualViewport;
  }

  /** Deprecated size of scrollable area. Is in DP. Use `cssContentSize` instead. */
  public Rect getContentSize() {
    return contentSize;
  }

  /** Deprecated size of scrollable area. Is in DP. Use `cssContentSize` instead. */
  public void setContentSize(Rect contentSize) {
    this.contentSize = contentSize;
  }

  /** Metrics relating to the layout viewport in CSS pixels. */
  public LayoutViewport getCssLayoutViewport() {
    return cssLayoutViewport;
  }

  /** Metrics relating to the layout viewport in CSS pixels. */
  public void setCssLayoutViewport(LayoutViewport cssLayoutViewport) {
    this.cssLayoutViewport = cssLayoutViewport;
  }

  /** Metrics relating to the visual viewport in CSS pixels. */
  public VisualViewport getCssVisualViewport() {
    return cssVisualViewport;
  }

  /** Metrics relating to the visual viewport in CSS pixels. */
  public void setCssVisualViewport(VisualViewport cssVisualViewport) {
    this.cssVisualViewport = cssVisualViewport;
  }

  /** Size of scrollable area in CSS pixels. */
  public Rect getCssContentSize() {
    return cssContentSize;
  }

  /** Size of scrollable area in CSS pixels. */
  public void setCssContentSize(Rect cssContentSize) {
    this.cssContentSize = cssContentSize;
  }
}
