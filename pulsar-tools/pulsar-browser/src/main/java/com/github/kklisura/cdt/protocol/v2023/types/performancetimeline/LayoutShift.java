package com.github.kklisura.cdt.protocol.v2023.types.performancetimeline;

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

/** See https://wicg.github.io/layout-instability/#sec-layout-shift and layout_shift.idl */
public class LayoutShift {

  private Double value;

  private Boolean hadRecentInput;

  private Double lastInputTime;

  private List<LayoutShiftAttribution> sources;

  /** Score increment produced by this event. */
  public Double getValue() {
    return value;
  }

  /** Score increment produced by this event. */
  public void setValue(Double value) {
    this.value = value;
  }

  public Boolean getHadRecentInput() {
    return hadRecentInput;
  }

  public void setHadRecentInput(Boolean hadRecentInput) {
    this.hadRecentInput = hadRecentInput;
  }

  public Double getLastInputTime() {
    return lastInputTime;
  }

  public void setLastInputTime(Double lastInputTime) {
    this.lastInputTime = lastInputTime;
  }

  public List<LayoutShiftAttribution> getSources() {
    return sources;
  }

  public void setSources(List<LayoutShiftAttribution> sources) {
    this.sources = sources;
  }
}
