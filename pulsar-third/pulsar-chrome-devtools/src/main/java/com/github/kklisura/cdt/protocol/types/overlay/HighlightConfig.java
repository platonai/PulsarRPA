package com.github.kklisura.cdt.protocol.types.overlay;

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

import com.github.kklisura.cdt.protocol.support.annotations.Optional;
import com.github.kklisura.cdt.protocol.types.dom.RGBA;

/** Configuration data for the highlighting of page elements. */
public class HighlightConfig {

  @Optional private Boolean showInfo;

  @Optional private Boolean showStyles;

  @Optional private Boolean showRulers;

  @Optional private Boolean showExtensionLines;

  @Optional private RGBA contentColor;

  @Optional private RGBA paddingColor;

  @Optional private RGBA borderColor;

  @Optional private RGBA marginColor;

  @Optional private RGBA eventTargetColor;

  @Optional private RGBA shapeColor;

  @Optional private RGBA shapeMarginColor;

  @Optional private RGBA cssGridColor;

  /** Whether the node info tooltip should be shown (default: false). */
  public Boolean getShowInfo() {
    return showInfo;
  }

  /** Whether the node info tooltip should be shown (default: false). */
  public void setShowInfo(Boolean showInfo) {
    this.showInfo = showInfo;
  }

  /** Whether the node styles in the tooltip (default: false). */
  public Boolean getShowStyles() {
    return showStyles;
  }

  /** Whether the node styles in the tooltip (default: false). */
  public void setShowStyles(Boolean showStyles) {
    this.showStyles = showStyles;
  }

  /** Whether the rulers should be shown (default: false). */
  public Boolean getShowRulers() {
    return showRulers;
  }

  /** Whether the rulers should be shown (default: false). */
  public void setShowRulers(Boolean showRulers) {
    this.showRulers = showRulers;
  }

  /** Whether the extension lines from node to the rulers should be shown (default: false). */
  public Boolean getShowExtensionLines() {
    return showExtensionLines;
  }

  /** Whether the extension lines from node to the rulers should be shown (default: false). */
  public void setShowExtensionLines(Boolean showExtensionLines) {
    this.showExtensionLines = showExtensionLines;
  }

  /** The content box highlight fill color (default: transparent). */
  public RGBA getContentColor() {
    return contentColor;
  }

  /** The content box highlight fill color (default: transparent). */
  public void setContentColor(RGBA contentColor) {
    this.contentColor = contentColor;
  }

  /** The padding highlight fill color (default: transparent). */
  public RGBA getPaddingColor() {
    return paddingColor;
  }

  /** The padding highlight fill color (default: transparent). */
  public void setPaddingColor(RGBA paddingColor) {
    this.paddingColor = paddingColor;
  }

  /** The border highlight fill color (default: transparent). */
  public RGBA getBorderColor() {
    return borderColor;
  }

  /** The border highlight fill color (default: transparent). */
  public void setBorderColor(RGBA borderColor) {
    this.borderColor = borderColor;
  }

  /** The margin highlight fill color (default: transparent). */
  public RGBA getMarginColor() {
    return marginColor;
  }

  /** The margin highlight fill color (default: transparent). */
  public void setMarginColor(RGBA marginColor) {
    this.marginColor = marginColor;
  }

  /** The event target element highlight fill color (default: transparent). */
  public RGBA getEventTargetColor() {
    return eventTargetColor;
  }

  /** The event target element highlight fill color (default: transparent). */
  public void setEventTargetColor(RGBA eventTargetColor) {
    this.eventTargetColor = eventTargetColor;
  }

  /** The shape outside fill color (default: transparent). */
  public RGBA getShapeColor() {
    return shapeColor;
  }

  /** The shape outside fill color (default: transparent). */
  public void setShapeColor(RGBA shapeColor) {
    this.shapeColor = shapeColor;
  }

  /** The shape margin fill color (default: transparent). */
  public RGBA getShapeMarginColor() {
    return shapeMarginColor;
  }

  /** The shape margin fill color (default: transparent). */
  public void setShapeMarginColor(RGBA shapeMarginColor) {
    this.shapeMarginColor = shapeMarginColor;
  }

  /** The grid layout color (default: transparent). */
  public RGBA getCssGridColor() {
    return cssGridColor;
  }

  /** The grid layout color (default: transparent). */
  public void setCssGridColor(RGBA cssGridColor) {
    this.cssGridColor = cssGridColor;
  }
}
