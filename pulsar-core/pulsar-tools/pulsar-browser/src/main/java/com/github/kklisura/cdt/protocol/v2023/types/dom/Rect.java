package com.github.kklisura.cdt.protocol.v2023.types.dom;

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

/** Rectangle. */
public class Rect {

  private Double x;

  private Double y;

  private Double width;

  private Double height;

  /** X coordinate */
  public Double getX() {
    return x;
  }

  /** X coordinate */
  public void setX(Double x) {
    this.x = x;
  }

  /** Y coordinate */
  public Double getY() {
    return y;
  }

  /** Y coordinate */
  public void setY(Double y) {
    this.y = y;
  }

  /** Rectangle width */
  public Double getWidth() {
    return width;
  }

  /** Rectangle width */
  public void setWidth(Double width) {
    this.width = width;
  }

  /** Rectangle height */
  public Double getHeight() {
    return height;
  }

  /** Rectangle height */
  public void setHeight(Double height) {
    this.height = height;
  }
}
