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

public class ScrollSnapContainerHighlightConfig {

  @Optional
  private LineStyle snapportBorder;

  @Optional private LineStyle snapAreaBorder;

  @Optional private RGBA scrollMarginColor;

  @Optional private RGBA scrollPaddingColor;

  /** The style of the snapport border (default: transparent) */
  public LineStyle getSnapportBorder() {
    return snapportBorder;
  }

  /** The style of the snapport border (default: transparent) */
  public void setSnapportBorder(LineStyle snapportBorder) {
    this.snapportBorder = snapportBorder;
  }

  /** The style of the snap area border (default: transparent) */
  public LineStyle getSnapAreaBorder() {
    return snapAreaBorder;
  }

  /** The style of the snap area border (default: transparent) */
  public void setSnapAreaBorder(LineStyle snapAreaBorder) {
    this.snapAreaBorder = snapAreaBorder;
  }

  /** The margin highlight fill color (default: transparent). */
  public RGBA getScrollMarginColor() {
    return scrollMarginColor;
  }

  /** The margin highlight fill color (default: transparent). */
  public void setScrollMarginColor(RGBA scrollMarginColor) {
    this.scrollMarginColor = scrollMarginColor;
  }

  /** The padding highlight fill color (default: transparent). */
  public RGBA getScrollPaddingColor() {
    return scrollPaddingColor;
  }

  /** The padding highlight fill color (default: transparent). */
  public void setScrollPaddingColor(RGBA scrollPaddingColor) {
    this.scrollPaddingColor = scrollPaddingColor;
  }
}
