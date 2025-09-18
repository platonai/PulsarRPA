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

import com.github.kklisura.cdt.protocol.v2023.support.annotations.Optional;

/** Visual viewport position, dimensions, and scale. */
public class VisualViewport {

  private Double offsetX;

  private Double offsetY;

  private Double pageX;

  private Double pageY;

  private Double clientWidth;

  private Double clientHeight;

  private Double scale;

  @Optional
  private Double zoom;

  /** Horizontal offset relative to the layout viewport (CSS pixels). */
  public Double getOffsetX() {
    return offsetX;
  }

  /** Horizontal offset relative to the layout viewport (CSS pixels). */
  public void setOffsetX(Double offsetX) {
    this.offsetX = offsetX;
  }

  /** Vertical offset relative to the layout viewport (CSS pixels). */
  public Double getOffsetY() {
    return offsetY;
  }

  /** Vertical offset relative to the layout viewport (CSS pixels). */
  public void setOffsetY(Double offsetY) {
    this.offsetY = offsetY;
  }

  /** Horizontal offset relative to the document (CSS pixels). */
  public Double getPageX() {
    return pageX;
  }

  /** Horizontal offset relative to the document (CSS pixels). */
  public void setPageX(Double pageX) {
    this.pageX = pageX;
  }

  /** Vertical offset relative to the document (CSS pixels). */
  public Double getPageY() {
    return pageY;
  }

  /** Vertical offset relative to the document (CSS pixels). */
  public void setPageY(Double pageY) {
    this.pageY = pageY;
  }

  /** Width (CSS pixels), excludes scrollbar if present. */
  public Double getClientWidth() {
    return clientWidth;
  }

  /** Width (CSS pixels), excludes scrollbar if present. */
  public void setClientWidth(Double clientWidth) {
    this.clientWidth = clientWidth;
  }

  /** Height (CSS pixels), excludes scrollbar if present. */
  public Double getClientHeight() {
    return clientHeight;
  }

  /** Height (CSS pixels), excludes scrollbar if present. */
  public void setClientHeight(Double clientHeight) {
    this.clientHeight = clientHeight;
  }

  /** Scale relative to the ideal viewport (size at width=device-width). */
  public Double getScale() {
    return scale;
  }

  /** Scale relative to the ideal viewport (size at width=device-width). */
  public void setScale(Double scale) {
    this.scale = scale;
  }

  /** Page zoom factor (CSS to device independent pixels ratio). */
  public Double getZoom() {
    return zoom;
  }

  /** Page zoom factor (CSS to device independent pixels ratio). */
  public void setZoom(Double zoom) {
    this.zoom = zoom;
  }
}
