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

import com.github.kklisura.cdt.protocol.support.annotations.Experimental;
import com.github.kklisura.cdt.protocol.support.annotations.Optional;

/** Screencast frame metadata. */
@Experimental
public class ScreencastFrameMetadata {

  private Double offsetTop;

  private Double pageScaleFactor;

  private Double deviceWidth;

  private Double deviceHeight;

  private Double scrollOffsetX;

  private Double scrollOffsetY;

  @Optional private Double timestamp;

  /** Top offset in DIP. */
  public Double getOffsetTop() {
    return offsetTop;
  }

  /** Top offset in DIP. */
  public void setOffsetTop(Double offsetTop) {
    this.offsetTop = offsetTop;
  }

  /** Page scale factor. */
  public Double getPageScaleFactor() {
    return pageScaleFactor;
  }

  /** Page scale factor. */
  public void setPageScaleFactor(Double pageScaleFactor) {
    this.pageScaleFactor = pageScaleFactor;
  }

  /** Device screen width in DIP. */
  public Double getDeviceWidth() {
    return deviceWidth;
  }

  /** Device screen width in DIP. */
  public void setDeviceWidth(Double deviceWidth) {
    this.deviceWidth = deviceWidth;
  }

  /** Device screen height in DIP. */
  public Double getDeviceHeight() {
    return deviceHeight;
  }

  /** Device screen height in DIP. */
  public void setDeviceHeight(Double deviceHeight) {
    this.deviceHeight = deviceHeight;
  }

  /** Position of horizontal scroll in CSS pixels. */
  public Double getScrollOffsetX() {
    return scrollOffsetX;
  }

  /** Position of horizontal scroll in CSS pixels. */
  public void setScrollOffsetX(Double scrollOffsetX) {
    this.scrollOffsetX = scrollOffsetX;
  }

  /** Position of vertical scroll in CSS pixels. */
  public Double getScrollOffsetY() {
    return scrollOffsetY;
  }

  /** Position of vertical scroll in CSS pixels. */
  public void setScrollOffsetY(Double scrollOffsetY) {
    this.scrollOffsetY = scrollOffsetY;
  }

  /** Frame swap timestamp. */
  public Double getTimestamp() {
    return timestamp;
  }

  /** Frame swap timestamp. */
  public void setTimestamp(Double timestamp) {
    this.timestamp = timestamp;
  }
}
