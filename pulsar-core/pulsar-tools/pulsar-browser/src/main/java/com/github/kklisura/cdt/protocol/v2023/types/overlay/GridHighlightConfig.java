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

/** Configuration data for the highlighting of Grid elements. */
public class GridHighlightConfig {

  @Optional
  private Boolean showGridExtensionLines;

  @Optional private Boolean showPositiveLineNumbers;

  @Optional private Boolean showNegativeLineNumbers;

  @Optional private Boolean showAreaNames;

  @Optional private Boolean showLineNames;

  @Optional private Boolean showTrackSizes;

  @Optional private RGBA gridBorderColor;

  @Deprecated @Optional private RGBA cellBorderColor;

  @Optional private RGBA rowLineColor;

  @Optional private RGBA columnLineColor;

  @Optional private Boolean gridBorderDash;

  @Deprecated @Optional private Boolean cellBorderDash;

  @Optional private Boolean rowLineDash;

  @Optional private Boolean columnLineDash;

  @Optional private RGBA rowGapColor;

  @Optional private RGBA rowHatchColor;

  @Optional private RGBA columnGapColor;

  @Optional private RGBA columnHatchColor;

  @Optional private RGBA areaBorderColor;

  @Optional private RGBA gridBackgroundColor;

  /** Whether the extension lines from grid cells to the rulers should be shown (default: false). */
  public Boolean getShowGridExtensionLines() {
    return showGridExtensionLines;
  }

  /** Whether the extension lines from grid cells to the rulers should be shown (default: false). */
  public void setShowGridExtensionLines(Boolean showGridExtensionLines) {
    this.showGridExtensionLines = showGridExtensionLines;
  }

  /** Show Positive line number labels (default: false). */
  public Boolean getShowPositiveLineNumbers() {
    return showPositiveLineNumbers;
  }

  /** Show Positive line number labels (default: false). */
  public void setShowPositiveLineNumbers(Boolean showPositiveLineNumbers) {
    this.showPositiveLineNumbers = showPositiveLineNumbers;
  }

  /** Show Negative line number labels (default: false). */
  public Boolean getShowNegativeLineNumbers() {
    return showNegativeLineNumbers;
  }

  /** Show Negative line number labels (default: false). */
  public void setShowNegativeLineNumbers(Boolean showNegativeLineNumbers) {
    this.showNegativeLineNumbers = showNegativeLineNumbers;
  }

  /** Show area name labels (default: false). */
  public Boolean getShowAreaNames() {
    return showAreaNames;
  }

  /** Show area name labels (default: false). */
  public void setShowAreaNames(Boolean showAreaNames) {
    this.showAreaNames = showAreaNames;
  }

  /** Show line name labels (default: false). */
  public Boolean getShowLineNames() {
    return showLineNames;
  }

  /** Show line name labels (default: false). */
  public void setShowLineNames(Boolean showLineNames) {
    this.showLineNames = showLineNames;
  }

  /** Show track size labels (default: false). */
  public Boolean getShowTrackSizes() {
    return showTrackSizes;
  }

  /** Show track size labels (default: false). */
  public void setShowTrackSizes(Boolean showTrackSizes) {
    this.showTrackSizes = showTrackSizes;
  }

  /** The grid container border highlight color (default: transparent). */
  public RGBA getGridBorderColor() {
    return gridBorderColor;
  }

  /** The grid container border highlight color (default: transparent). */
  public void setGridBorderColor(RGBA gridBorderColor) {
    this.gridBorderColor = gridBorderColor;
  }

  /**
   * The cell border color (default: transparent). Deprecated, please use rowLineColor and
   * columnLineColor instead.
   */
  public RGBA getCellBorderColor() {
    return cellBorderColor;
  }

  /**
   * The cell border color (default: transparent). Deprecated, please use rowLineColor and
   * columnLineColor instead.
   */
  public void setCellBorderColor(RGBA cellBorderColor) {
    this.cellBorderColor = cellBorderColor;
  }

  /** The row line color (default: transparent). */
  public RGBA getRowLineColor() {
    return rowLineColor;
  }

  /** The row line color (default: transparent). */
  public void setRowLineColor(RGBA rowLineColor) {
    this.rowLineColor = rowLineColor;
  }

  /** The column line color (default: transparent). */
  public RGBA getColumnLineColor() {
    return columnLineColor;
  }

  /** The column line color (default: transparent). */
  public void setColumnLineColor(RGBA columnLineColor) {
    this.columnLineColor = columnLineColor;
  }

  /** Whether the grid border is dashed (default: false). */
  public Boolean getGridBorderDash() {
    return gridBorderDash;
  }

  /** Whether the grid border is dashed (default: false). */
  public void setGridBorderDash(Boolean gridBorderDash) {
    this.gridBorderDash = gridBorderDash;
  }

  /**
   * Whether the cell border is dashed (default: false). Deprecated, please us rowLineDash and
   * columnLineDash instead.
   */
  public Boolean getCellBorderDash() {
    return cellBorderDash;
  }

  /**
   * Whether the cell border is dashed (default: false). Deprecated, please us rowLineDash and
   * columnLineDash instead.
   */
  public void setCellBorderDash(Boolean cellBorderDash) {
    this.cellBorderDash = cellBorderDash;
  }

  /** Whether row lines are dashed (default: false). */
  public Boolean getRowLineDash() {
    return rowLineDash;
  }

  /** Whether row lines are dashed (default: false). */
  public void setRowLineDash(Boolean rowLineDash) {
    this.rowLineDash = rowLineDash;
  }

  /** Whether column lines are dashed (default: false). */
  public Boolean getColumnLineDash() {
    return columnLineDash;
  }

  /** Whether column lines are dashed (default: false). */
  public void setColumnLineDash(Boolean columnLineDash) {
    this.columnLineDash = columnLineDash;
  }

  /** The row gap highlight fill color (default: transparent). */
  public RGBA getRowGapColor() {
    return rowGapColor;
  }

  /** The row gap highlight fill color (default: transparent). */
  public void setRowGapColor(RGBA rowGapColor) {
    this.rowGapColor = rowGapColor;
  }

  /** The row gap hatching fill color (default: transparent). */
  public RGBA getRowHatchColor() {
    return rowHatchColor;
  }

  /** The row gap hatching fill color (default: transparent). */
  public void setRowHatchColor(RGBA rowHatchColor) {
    this.rowHatchColor = rowHatchColor;
  }

  /** The column gap highlight fill color (default: transparent). */
  public RGBA getColumnGapColor() {
    return columnGapColor;
  }

  /** The column gap highlight fill color (default: transparent). */
  public void setColumnGapColor(RGBA columnGapColor) {
    this.columnGapColor = columnGapColor;
  }

  /** The column gap hatching fill color (default: transparent). */
  public RGBA getColumnHatchColor() {
    return columnHatchColor;
  }

  /** The column gap hatching fill color (default: transparent). */
  public void setColumnHatchColor(RGBA columnHatchColor) {
    this.columnHatchColor = columnHatchColor;
  }

  /** The named grid areas border color (Default: transparent). */
  public RGBA getAreaBorderColor() {
    return areaBorderColor;
  }

  /** The named grid areas border color (Default: transparent). */
  public void setAreaBorderColor(RGBA areaBorderColor) {
    this.areaBorderColor = areaBorderColor;
  }

  /** The grid container background color (Default: transparent). */
  public RGBA getGridBackgroundColor() {
    return gridBackgroundColor;
  }

  /** The grid container background color (Default: transparent). */
  public void setGridBackgroundColor(RGBA gridBackgroundColor) {
    this.gridBackgroundColor = gridBackgroundColor;
  }
}
