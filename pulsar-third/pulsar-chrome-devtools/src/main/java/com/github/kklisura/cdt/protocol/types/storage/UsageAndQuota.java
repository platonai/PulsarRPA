package com.github.kklisura.cdt.protocol.types.storage;

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

import java.util.List;

public class UsageAndQuota {

  private Double usage;

  private Double quota;

  private List<UsageForType> usageBreakdown;

  /** Storage usage (bytes). */
  public Double getUsage() {
    return usage;
  }

  /** Storage usage (bytes). */
  public void setUsage(Double usage) {
    this.usage = usage;
  }

  /** Storage quota (bytes). */
  public Double getQuota() {
    return quota;
  }

  /** Storage quota (bytes). */
  public void setQuota(Double quota) {
    this.quota = quota;
  }

  /** Storage usage per type (bytes). */
  public List<UsageForType> getUsageBreakdown() {
    return usageBreakdown;
  }

  /** Storage usage per type (bytes). */
  public void setUsageBreakdown(List<UsageForType> usageBreakdown) {
    this.usageBreakdown = usageBreakdown;
  }
}
