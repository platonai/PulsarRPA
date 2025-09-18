package com.github.kklisura.cdt.protocol.v2023.types.page;

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

@Experimental
public class OriginTrial {

  private String trialName;

  private OriginTrialStatus status;

  private List<OriginTrialTokenWithStatus> tokensWithStatus;

  public String getTrialName() {
    return trialName;
  }

  public void setTrialName(String trialName) {
    this.trialName = trialName;
  }

  public OriginTrialStatus getStatus() {
    return status;
  }

  public void setStatus(OriginTrialStatus status) {
    this.status = status;
  }

  public List<OriginTrialTokenWithStatus> getTokensWithStatus() {
    return tokensWithStatus;
  }

  public void setTokensWithStatus(List<OriginTrialTokenWithStatus> tokensWithStatus) {
    this.tokensWithStatus = tokensWithStatus;
  }
}
