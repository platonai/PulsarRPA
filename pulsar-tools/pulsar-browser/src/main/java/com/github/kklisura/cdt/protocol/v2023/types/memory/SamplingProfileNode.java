package com.github.kklisura.cdt.protocol.v2023.types.memory;

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

/** Heap profile sample. */
public class SamplingProfileNode {

  private Double size;

  private Double total;

  private List<String> stack;

  /** Size of the sampled allocation. */
  public Double getSize() {
    return size;
  }

  /** Size of the sampled allocation. */
  public void setSize(Double size) {
    this.size = size;
  }

  /** Total bytes attributed to this sample. */
  public Double getTotal() {
    return total;
  }

  /** Total bytes attributed to this sample. */
  public void setTotal(Double total) {
    this.total = total;
  }

  /** Execution stack at the point of allocation. */
  public List<String> getStack() {
    return stack;
  }

  /** Execution stack at the point of allocation. */
  public void setStack(List<String> stack) {
    this.stack = stack;
  }
}
