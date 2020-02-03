package com.github.kklisura.cdt.protocol.types.indexeddb;

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

/** Key range. */
public class KeyRange {

  @Optional private Key lower;

  @Optional private Key upper;

  private Boolean lowerOpen;

  private Boolean upperOpen;

  /** Lower bound. */
  public Key getLower() {
    return lower;
  }

  /** Lower bound. */
  public void setLower(Key lower) {
    this.lower = lower;
  }

  /** Upper bound. */
  public Key getUpper() {
    return upper;
  }

  /** Upper bound. */
  public void setUpper(Key upper) {
    this.upper = upper;
  }

  /** If true lower bound is open. */
  public Boolean getLowerOpen() {
    return lowerOpen;
  }

  /** If true lower bound is open. */
  public void setLowerOpen(Boolean lowerOpen) {
    this.lowerOpen = lowerOpen;
  }

  /** If true upper bound is open. */
  public Boolean getUpperOpen() {
    return upperOpen;
  }

  /** If true upper bound is open. */
  public void setUpperOpen(Boolean upperOpen) {
    this.upperOpen = upperOpen;
  }
}
