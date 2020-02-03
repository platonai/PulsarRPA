package com.github.kklisura.cdt.protocol.types.runtime;

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

public class HeapUsage {

  private Double usedSize;

  private Double totalSize;

  /** Used heap size in bytes. */
  public Double getUsedSize() {
    return usedSize;
  }

  /** Used heap size in bytes. */
  public void setUsedSize(Double usedSize) {
    this.usedSize = usedSize;
  }

  /** Allocated heap size in bytes. */
  public Double getTotalSize() {
    return totalSize;
  }

  /** Allocated heap size in bytes. */
  public void setTotalSize(Double totalSize) {
    this.totalSize = totalSize;
  }
}
