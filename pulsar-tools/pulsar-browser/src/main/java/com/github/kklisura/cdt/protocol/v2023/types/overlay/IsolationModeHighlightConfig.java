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

public class IsolationModeHighlightConfig {

  @Optional
  private RGBA resizerColor;

  @Optional private RGBA resizerHandleColor;

  @Optional private RGBA maskColor;

  /** The fill color of the resizers (default: transparent). */
  public RGBA getResizerColor() {
    return resizerColor;
  }

  /** The fill color of the resizers (default: transparent). */
  public void setResizerColor(RGBA resizerColor) {
    this.resizerColor = resizerColor;
  }

  /** The fill color for resizer handles (default: transparent). */
  public RGBA getResizerHandleColor() {
    return resizerHandleColor;
  }

  /** The fill color for resizer handles (default: transparent). */
  public void setResizerHandleColor(RGBA resizerHandleColor) {
    this.resizerHandleColor = resizerHandleColor;
  }

  /** The fill color for the mask covering non-isolated elements (default: transparent). */
  public RGBA getMaskColor() {
    return maskColor;
  }

  /** The fill color for the mask covering non-isolated elements (default: transparent). */
  public void setMaskColor(RGBA maskColor) {
    this.maskColor = maskColor;
  }
}
