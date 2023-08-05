package com.github.kklisura.cdt.protocol.v2023.types.browser;

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

public class WindowForTarget {

  private Integer windowId;

  private Bounds bounds;

  /** Browser window id. */
  public Integer getWindowId() {
    return windowId;
  }

  /** Browser window id. */
  public void setWindowId(Integer windowId) {
    this.windowId = windowId;
  }

  /**
   * Bounds information of the window. When window state is 'minimized', the restored window
   * position and size are returned.
   */
  public Bounds getBounds() {
    return bounds;
  }

  /**
   * Bounds information of the window. When window state is 'minimized', the restored window
   * position and size are returned.
   */
  public void setBounds(Bounds bounds) {
    this.bounds = bounds;
  }
}
