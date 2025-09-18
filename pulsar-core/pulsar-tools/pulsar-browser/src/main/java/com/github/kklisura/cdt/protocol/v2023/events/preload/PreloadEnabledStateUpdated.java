package com.github.kklisura.cdt.protocol.v2023.events.preload;

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

/** Fired when a preload enabled state is updated. */
public class PreloadEnabledStateUpdated {

  private Boolean disabledByPreference;

  private Boolean disabledByDataSaver;

  private Boolean disabledByBatterySaver;

  private Boolean disabledByHoldbackPrefetchSpeculationRules;

  private Boolean disabledByHoldbackPrerenderSpeculationRules;

  public Boolean getDisabledByPreference() {
    return disabledByPreference;
  }

  public void setDisabledByPreference(Boolean disabledByPreference) {
    this.disabledByPreference = disabledByPreference;
  }

  public Boolean getDisabledByDataSaver() {
    return disabledByDataSaver;
  }

  public void setDisabledByDataSaver(Boolean disabledByDataSaver) {
    this.disabledByDataSaver = disabledByDataSaver;
  }

  public Boolean getDisabledByBatterySaver() {
    return disabledByBatterySaver;
  }

  public void setDisabledByBatterySaver(Boolean disabledByBatterySaver) {
    this.disabledByBatterySaver = disabledByBatterySaver;
  }

  public Boolean getDisabledByHoldbackPrefetchSpeculationRules() {
    return disabledByHoldbackPrefetchSpeculationRules;
  }

  public void setDisabledByHoldbackPrefetchSpeculationRules(
      Boolean disabledByHoldbackPrefetchSpeculationRules) {
    this.disabledByHoldbackPrefetchSpeculationRules = disabledByHoldbackPrefetchSpeculationRules;
  }

  public Boolean getDisabledByHoldbackPrerenderSpeculationRules() {
    return disabledByHoldbackPrerenderSpeculationRules;
  }

  public void setDisabledByHoldbackPrerenderSpeculationRules(
      Boolean disabledByHoldbackPrerenderSpeculationRules) {
    this.disabledByHoldbackPrerenderSpeculationRules = disabledByHoldbackPrerenderSpeculationRules;
  }
}
