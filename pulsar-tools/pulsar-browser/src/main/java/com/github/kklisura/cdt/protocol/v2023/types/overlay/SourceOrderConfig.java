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

import com.github.kklisura.cdt.protocol.v2023.types.dom.RGBA;

/** Configuration data for drawing the source order of an elements children. */
public class SourceOrderConfig {

  private RGBA parentOutlineColor;

  private RGBA childOutlineColor;

  /** the color to outline the givent element in. */
  public RGBA getParentOutlineColor() {
    return parentOutlineColor;
  }

  /** the color to outline the givent element in. */
  public void setParentOutlineColor(RGBA parentOutlineColor) {
    this.parentOutlineColor = parentOutlineColor;
  }

  /** the color to outline the child elements in. */
  public RGBA getChildOutlineColor() {
    return childOutlineColor;
  }

  /** the color to outline the child elements in. */
  public void setChildOutlineColor(RGBA childOutlineColor) {
    this.childOutlineColor = childOutlineColor;
  }
}
