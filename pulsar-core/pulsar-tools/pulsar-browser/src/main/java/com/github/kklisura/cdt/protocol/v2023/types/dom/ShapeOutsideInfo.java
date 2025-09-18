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

import java.util.List;

/** CSS Shape Outside details. */
public class ShapeOutsideInfo {

  private List<Double> bounds;

  private List<Object> shape;

  private List<Object> marginShape;

  /** Shape bounds */
  public List<Double> getBounds() {
    return bounds;
  }

  /** Shape bounds */
  public void setBounds(List<Double> bounds) {
    this.bounds = bounds;
  }

  /** Shape coordinate details */
  public List<Object> getShape() {
    return shape;
  }

  /** Shape coordinate details */
  public void setShape(List<Object> shape) {
    this.shape = shape;
  }

  /** Margin shape bounds */
  public List<Object> getMarginShape() {
    return marginShape;
  }

  /** Margin shape bounds */
  public void setMarginShape(List<Object> marginShape) {
    this.marginShape = marginShape;
  }
}
