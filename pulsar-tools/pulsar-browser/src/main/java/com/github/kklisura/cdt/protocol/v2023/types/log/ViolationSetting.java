package com.github.kklisura.cdt.protocol.v2023.types.log;

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

/** Violation configuration setting. */
public class ViolationSetting {

  private ViolationSettingName name;

  private Double threshold;

  /** Violation type. */
  public ViolationSettingName getName() {
    return name;
  }

  /** Violation type. */
  public void setName(ViolationSettingName name) {
    this.name = name;
  }

  /** Time threshold to trigger upon. */
  public Double getThreshold() {
    return threshold;
  }

  /** Time threshold to trigger upon. */
  public void setThreshold(Double threshold) {
    this.threshold = threshold;
  }
}
