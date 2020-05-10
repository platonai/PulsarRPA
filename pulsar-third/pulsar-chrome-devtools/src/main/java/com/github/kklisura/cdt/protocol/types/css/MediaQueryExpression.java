package com.github.kklisura.cdt.protocol.types.css;

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

/** Media query expression descriptor. */
public class MediaQueryExpression {

  private Double value;

  private String unit;

  private String feature;

  @Optional private SourceRange valueRange;

  @Optional private Double computedLength;

  /** Media query expression value. */
  public Double getValue() {
    return value;
  }

  /** Media query expression value. */
  public void setValue(Double value) {
    this.value = value;
  }

  /** Media query expression units. */
  public String getUnit() {
    return unit;
  }

  /** Media query expression units. */
  public void setUnit(String unit) {
    this.unit = unit;
  }

  /** Media query expression feature. */
  public String getFeature() {
    return feature;
  }

  /** Media query expression feature. */
  public void setFeature(String feature) {
    this.feature = feature;
  }

  /** The associated range of the value text in the enclosing stylesheet (if available). */
  public SourceRange getValueRange() {
    return valueRange;
  }

  /** The associated range of the value text in the enclosing stylesheet (if available). */
  public void setValueRange(SourceRange valueRange) {
    this.valueRange = valueRange;
  }

  /** Computed length of media query expression (if applicable). */
  public Double getComputedLength() {
    return computedLength;
  }

  /** Computed length of media query expression (if applicable). */
  public void setComputedLength(Double computedLength) {
    this.computedLength = computedLength;
  }
}
