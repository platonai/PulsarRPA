package com.github.kklisura.cdt.protocol.types.profiler;

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

/** Specifies a number of samples attributed to a certain source position. */
public class PositionTickInfo {

  private Integer line;

  private Integer ticks;

  /** Source line number (1-based). */
  public Integer getLine() {
    return line;
  }

  /** Source line number (1-based). */
  public void setLine(Integer line) {
    this.line = line;
  }

  /** Number of samples attributed to the source line. */
  public Integer getTicks() {
    return ticks;
  }

  /** Number of samples attributed to the source line. */
  public void setTicks(Integer ticks) {
    this.ticks = ticks;
  }
}
