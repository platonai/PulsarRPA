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

/** A structure holding an RGBA color. */
public class RGBA {

  private Integer r;

  private Integer g;

  private Integer b;

  @Optional
  private Double a;

  /** The red component, in the [0-255] range. */
  public Integer getR() {
    return r;
  }

  /** The red component, in the [0-255] range. */
  public void setR(Integer r) {
    this.r = r;
  }

  /** The green component, in the [0-255] range. */
  public Integer getG() {
    return g;
  }

  /** The green component, in the [0-255] range. */
  public void setG(Integer g) {
    this.g = g;
  }

  /** The blue component, in the [0-255] range. */
  public Integer getB() {
    return b;
  }

  /** The blue component, in the [0-255] range. */
  public void setB(Integer b) {
    this.b = b;
  }

  /** The alpha component, in the [0-1] range (default: 1). */
  public Double getA() {
    return a;
  }

  /** The alpha component, in the [0-1] range (default: 1). */
  public void setA(Double a) {
    this.a = a;
  }
}
