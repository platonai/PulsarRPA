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

import java.util.List;

/** Chrome histogram. */
@Experimental
public class Histogram {

  private String name;

  private Integer sum;

  private Integer count;

  private List<Bucket> buckets;

  /** Name. */
  public String getName() {
    return name;
  }

  /** Name. */
  public void setName(String name) {
    this.name = name;
  }

  /** Sum of sample values. */
  public Integer getSum() {
    return sum;
  }

  /** Sum of sample values. */
  public void setSum(Integer sum) {
    this.sum = sum;
  }

  /** Total number of samples. */
  public Integer getCount() {
    return count;
  }

  /** Total number of samples. */
  public void setCount(Integer count) {
    this.count = count;
  }

  /** Buckets. */
  public List<Bucket> getBuckets() {
    return buckets;
  }

  /** Buckets. */
  public void setBuckets(List<Bucket> buckets) {
    this.buckets = buckets;
  }
}
