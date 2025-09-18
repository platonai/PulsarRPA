package com.github.kklisura.cdt.protocol.v2023.types.css;

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

import com.github.kklisura.cdt.protocol.v2023.support.annotations.Experimental;

/** Specificity: https://drafts.csswg.org/selectors/#specificity-rules */
@Experimental
public class Specificity {

  private Integer a;

  private Integer b;

  private Integer c;

  /** The a component, which represents the number of ID selectors. */
  public Integer getA() {
    return a;
  }

  /** The a component, which represents the number of ID selectors. */
  public void setA(Integer a) {
    this.a = a;
  }

  /**
   * The b component, which represents the number of class selectors, attributes selectors, and
   * pseudo-classes.
   */
  public Integer getB() {
    return b;
  }

  /**
   * The b component, which represents the number of class selectors, attributes selectors, and
   * pseudo-classes.
   */
  public void setB(Integer b) {
    this.b = b;
  }

  /** The c component, which represents the number of type selectors and pseudo-elements. */
  public Integer getC() {
    return c;
  }

  /** The c component, which represents the number of type selectors and pseudo-elements. */
  public void setC(Integer c) {
    this.c = c;
  }
}
