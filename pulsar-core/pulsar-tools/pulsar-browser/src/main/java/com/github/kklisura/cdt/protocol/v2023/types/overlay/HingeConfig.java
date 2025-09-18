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

import com.github.kklisura.cdt.protocol.v2023.support.annotations.Optional;
import com.github.kklisura.cdt.protocol.v2023.types.dom.RGBA;
import com.github.kklisura.cdt.protocol.v2023.types.dom.Rect;

/** Configuration for dual screen hinge */
public class HingeConfig {

  private Rect rect;

  @Optional
  private RGBA contentColor;

  @Optional private RGBA outlineColor;

  /** A rectangle represent hinge */
  public Rect getRect() {
    return rect;
  }

  /** A rectangle represent hinge */
  public void setRect(Rect rect) {
    this.rect = rect;
  }

  /** The content box highlight fill color (default: a dark color). */
  public RGBA getContentColor() {
    return contentColor;
  }

  /** The content box highlight fill color (default: a dark color). */
  public void setContentColor(RGBA contentColor) {
    this.contentColor = contentColor;
  }

  /** The content box highlight outline color (default: transparent). */
  public RGBA getOutlineColor() {
    return outlineColor;
  }

  /** The content box highlight outline color (default: transparent). */
  public void setOutlineColor(RGBA outlineColor) {
    this.outlineColor = outlineColor;
  }
}
