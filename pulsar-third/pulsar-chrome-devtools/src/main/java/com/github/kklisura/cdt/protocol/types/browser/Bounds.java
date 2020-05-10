package com.github.kklisura.cdt.protocol.types.browser;

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

import com.github.kklisura.cdt.protocol.support.annotations.Experimental;
import com.github.kklisura.cdt.protocol.support.annotations.Optional;

/** Browser window bounds information */
@Experimental
public class Bounds {

  @Optional private Integer left;

  @Optional private Integer top;

  @Optional private Integer width;

  @Optional private Integer height;

  @Optional private WindowState windowState;

  /** The offset from the left edge of the screen to the window in pixels. */
  public Integer getLeft() {
    return left;
  }

  /** The offset from the left edge of the screen to the window in pixels. */
  public void setLeft(Integer left) {
    this.left = left;
  }

  /** The offset from the top edge of the screen to the window in pixels. */
  public Integer getTop() {
    return top;
  }

  /** The offset from the top edge of the screen to the window in pixels. */
  public void setTop(Integer top) {
    this.top = top;
  }

  /** The window width in pixels. */
  public Integer getWidth() {
    return width;
  }

  /** The window width in pixels. */
  public void setWidth(Integer width) {
    this.width = width;
  }

  /** The window height in pixels. */
  public Integer getHeight() {
    return height;
  }

  /** The window height in pixels. */
  public void setHeight(Integer height) {
    this.height = height;
  }

  /** The window state. Default to normal. */
  public WindowState getWindowState() {
    return windowState;
  }

  /** The window state. Default to normal. */
  public void setWindowState(WindowState windowState) {
    this.windowState = windowState;
  }
}
