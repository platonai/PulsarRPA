package com.github.kklisura.cdt.protocol.v2023.types.browser;

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

/** Chrome histogram bucket. */
@Experimental
public class Bucket {

  private Integer low;

  private Integer high;

  private Integer count;

  /** Minimum value (inclusive). */
  public Integer getLow() {
    return low;
  }

  /** Minimum value (inclusive). */
  public void setLow(Integer low) {
    this.low = low;
  }

  /** Maximum value (exclusive). */
  public Integer getHigh() {
    return high;
  }

  /** Maximum value (exclusive). */
  public void setHigh(Integer high) {
    this.high = high;
  }

  /** Number of samples. */
  public Integer getCount() {
    return count;
  }

  /** Number of samples. */
  public void setCount(Integer count) {
    this.count = count;
  }
}
