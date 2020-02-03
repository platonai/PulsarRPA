package com.github.kklisura.cdt.protocol.types.systeminfo;

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

/** Represents process info. */
public class ProcessInfo {

  private String type;

  private Integer id;

  private Double cpuTime;

  /** Specifies process type. */
  public String getType() {
    return type;
  }

  /** Specifies process type. */
  public void setType(String type) {
    this.type = type;
  }

  /** Specifies process id. */
  public Integer getId() {
    return id;
  }

  /** Specifies process id. */
  public void setId(Integer id) {
    this.id = id;
  }

  /**
   * Specifies cumulative CPU usage in seconds across all threads of the process since the process
   * start.
   */
  public Double getCpuTime() {
    return cpuTime;
  }

  /**
   * Specifies cumulative CPU usage in seconds across all threads of the process since the process
   * start.
   */
  public void setCpuTime(Double cpuTime) {
    this.cpuTime = cpuTime;
  }
}
