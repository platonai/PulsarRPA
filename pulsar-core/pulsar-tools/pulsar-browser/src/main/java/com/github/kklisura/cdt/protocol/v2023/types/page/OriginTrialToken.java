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

@Experimental
public class OriginTrialToken {

  private String origin;

  private Boolean matchSubDomains;

  private String trialName;

  private Double expiryTime;

  private Boolean isThirdParty;

  private OriginTrialUsageRestriction usageRestriction;

  public String getOrigin() {
    return origin;
  }

  public void setOrigin(String origin) {
    this.origin = origin;
  }

  public Boolean getMatchSubDomains() {
    return matchSubDomains;
  }

  public void setMatchSubDomains(Boolean matchSubDomains) {
    this.matchSubDomains = matchSubDomains;
  }

  public String getTrialName() {
    return trialName;
  }

  public void setTrialName(String trialName) {
    this.trialName = trialName;
  }

  public Double getExpiryTime() {
    return expiryTime;
  }

  public void setExpiryTime(Double expiryTime) {
    this.expiryTime = expiryTime;
  }

  public Boolean getIsThirdParty() {
    return isThirdParty;
  }

  public void setIsThirdParty(Boolean isThirdParty) {
    this.isThirdParty = isThirdParty;
  }

  public OriginTrialUsageRestriction getUsageRestriction() {
    return usageRestriction;
  }

  public void setUsageRestriction(OriginTrialUsageRestriction usageRestriction) {
    this.usageRestriction = usageRestriction;
  }
}
