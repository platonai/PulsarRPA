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

/** Layout viewport position and dimensions. */
public class LayoutViewport {

  private Integer pageX;

  private Integer pageY;

  private Integer clientWidth;

  private Integer clientHeight;

  /** Horizontal offset relative to the document (CSS pixels). */
  public Integer getPageX() {
    return pageX;
  }

  /** Horizontal offset relative to the document (CSS pixels). */
  public void setPageX(Integer pageX) {
    this.pageX = pageX;
  }

  /** Vertical offset relative to the document (CSS pixels). */
  public Integer getPageY() {
    return pageY;
  }

  /** Vertical offset relative to the document (CSS pixels). */
  public void setPageY(Integer pageY) {
    this.pageY = pageY;
  }

  /** Width (CSS pixels), excludes scrollbar if present. */
  public Integer getClientWidth() {
    return clientWidth;
  }

  /** Width (CSS pixels), excludes scrollbar if present. */
  public void setClientWidth(Integer clientWidth) {
    this.clientWidth = clientWidth;
  }

  /** Height (CSS pixels), excludes scrollbar if present. */
  public Integer getClientHeight() {
    return clientHeight;
  }

  /** Height (CSS pixels), excludes scrollbar if present. */
  public void setClientHeight(Integer clientHeight) {
    this.clientHeight = clientHeight;
  }
}
