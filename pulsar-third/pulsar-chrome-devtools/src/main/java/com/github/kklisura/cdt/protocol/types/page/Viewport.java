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

/** Viewport for capturing screenshot. */
public class Viewport {

  private Double x;

  private Double y;

  private Double width;

  private Double height;

  private Double scale;

  /** X offset in device independent pixels (dip). */
  public Double getX() {
    return x;
  }

  /** X offset in device independent pixels (dip). */
  public void setX(Double x) {
    this.x = x;
  }

  /** Y offset in device independent pixels (dip). */
  public Double getY() {
    return y;
  }

  /** Y offset in device independent pixels (dip). */
  public void setY(Double y) {
    this.y = y;
  }

  /** Rectangle width in device independent pixels (dip). */
  public Double getWidth() {
    return width;
  }

  /** Rectangle width in device independent pixels (dip). */
  public void setWidth(Double width) {
    this.width = width;
  }

  /** Rectangle height in device independent pixels (dip). */
  public Double getHeight() {
    return height;
  }

  /** Rectangle height in device independent pixels (dip). */
  public void setHeight(Double height) {
    this.height = height;
  }

  /** Page scale factor. */
  public Double getScale() {
    return scale;
  }

  /** Page scale factor. */
  public void setScale(Double scale) {
    this.scale = scale;
  }
}
