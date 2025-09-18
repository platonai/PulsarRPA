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

import com.github.kklisura.cdt.protocol.v2023.support.annotations.Optional;

import java.util.List;

/** Box model. */
public class BoxModel {

  private List<Double> content;

  private List<Double> padding;

  private List<Double> border;

  private List<Double> margin;

  private Integer width;

  private Integer height;

  @Optional
  private ShapeOutsideInfo shapeOutside;

  /** Content box */
  public List<Double> getContent() {
    return content;
  }

  /** Content box */
  public void setContent(List<Double> content) {
    this.content = content;
  }

  /** Padding box */
  public List<Double> getPadding() {
    return padding;
  }

  /** Padding box */
  public void setPadding(List<Double> padding) {
    this.padding = padding;
  }

  /** Border box */
  public List<Double> getBorder() {
    return border;
  }

  /** Border box */
  public void setBorder(List<Double> border) {
    this.border = border;
  }

  /** Margin box */
  public List<Double> getMargin() {
    return margin;
  }

  /** Margin box */
  public void setMargin(List<Double> margin) {
    this.margin = margin;
  }

  /** Node width */
  public Integer getWidth() {
    return width;
  }

  /** Node width */
  public void setWidth(Integer width) {
    this.width = width;
  }

  /** Node height */
  public Integer getHeight() {
    return height;
  }

  /** Node height */
  public void setHeight(Integer height) {
    this.height = height;
  }

  /** Shape outside coordinates */
  public ShapeOutsideInfo getShapeOutside() {
    return shapeOutside;
  }

  /** Shape outside coordinates */
  public void setShapeOutside(ShapeOutsideInfo shapeOutside) {
    this.shapeOutside = shapeOutside;
  }
}
